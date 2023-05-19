package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.OutputObject;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.digitalcash.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

@Singleton
public class DigitalCashRepository {

  public static final String TAG = DigitalCashRepository.class.getSimpleName();

  private final Map<String, LaoTransactions> transactionsByLao = new HashMap<>();

  private final TransactionDao transactionDao;
  private final HashDao hashDao;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public DigitalCashRepository(AppDatabase appDatabase, Application application) {
    transactionDao = appDatabase.transactionDao();
    hashDao = appDatabase.hashDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  public List<TransactionObject> getTransactions(String laoId, PublicKey user) {
    return getLaoTransactions(laoId).getTransactions(user);
  }

  public Observable<List<TransactionObject>> getTransactionsObservable(
      String laoId, PublicKey user) {
    return getLaoTransactions(laoId).getTransactionsObservable(user);
  }

  public long getUserBalance(String laoId, PublicKey user) {
    return getLaoTransactions(laoId).getUserBalance(user);
  }

  public void updateTransactions(String laoId, TransactionObject transaction)
      throws NoRollCallException {
    Timber.tag(TAG).d("updating transactions on Lao %s and transaction %s", laoId, transaction);
    getLaoTransactions(laoId).updateTransactions(transaction, true);
  }

  public void initializeDigitalCash(String laoId, List<PublicKey> attendees) {
    getLaoTransactions(laoId).initializeDigitalCash(attendees);
  }

  private synchronized LaoTransactions getLaoTransactions(String laoId) {
    // Create the lao transactions object if it is not present yet
    return transactionsByLao.computeIfAbsent(laoId, lao -> new LaoTransactions(laoId, this));
  }

  private static final class LaoTransactions {

    private final String laoId;
    private final DigitalCashRepository repository;
    private boolean alreadyRetrieved = false;

    private final Map<PublicKey, List<TransactionObject>> transactions = new HashMap<>();
    private final Map<PublicKey, Subject<List<TransactionObject>>> transactionsSubject =
        new HashMap<>();
    private final Map<String, PublicKey> hashDictionary = new HashMap<>();

    public LaoTransactions(String laoId, DigitalCashRepository repository) {
      this.laoId = laoId;
      this.repository = repository;
    }

    /**
     * This resets the digital cash state. This should be called each time a rc is closed
     *
     * @param attendees the attendees of the closed roll call
     */
    public synchronized void initializeDigitalCash(List<PublicKey> attendees) {
      Timber.tag(TAG).d("initializing digital cash with attendees %s", attendees);
      // Clear the database for the given lao
      repository.disposables.add(
          repository
              .transactionDao
              .deleteByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () -> Timber.tag(TAG).d("Cleared the transactions in the db for lao %s", laoId),
                  err ->
                      Timber.tag(TAG).e(err, "Error in clearing transactions for lao %s", laoId)));
      repository.disposables.add(
          repository
              .hashDao
              .deleteByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () ->
                      Timber.tag(TAG).d("Cleared the hash dictionary in the db for lao %s", laoId),
                  err ->
                      Timber.tag(TAG)
                          .e(err, "Error in clearing the hash dictionary for lao %s", laoId)));

      // Clear the memory
      hashDictionary.clear();
      transactionsSubject.values().forEach(Observer::onComplete);
      transactionsSubject.clear();

      // Reset the hash dictionary
      attendees.forEach(
          publicKey -> {
            String hash = publicKey.computeHash();
            hashDictionary.put(hash, publicKey);
            // Save the mapping in the db
            HashEntity hashEntity = new HashEntity(hash, laoId, publicKey);
            repository.disposables.add(
                repository
                    .hashDao
                    .insert(hashEntity)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () ->
                            Timber.tag(TAG)
                                .d("Persisted hash %s in the db for lao %s", hash, laoId),
                        err ->
                            Timber.tag(TAG)
                                .e(
                                    err,
                                    "Error in persisting the hash %s for lao %s",
                                    hash,
                                    laoId)));
          });
    }

    public synchronized void updateTransactions(TransactionObject transaction, boolean toBeStored)
        throws NoRollCallException {
      if (hashDictionary.isEmpty()) {
        throw new NoRollCallException("No roll call attendees could be found");
      }

      if (toBeStored) {
        TransactionEntity transactionEntity = new TransactionEntity(laoId, transaction);
        repository.disposables.add(
            repository
                .transactionDao
                .insert(transactionEntity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {}, err -> {}));
      }

      for (PublicKey current : getReceiversTransaction(transaction)) {
        List<TransactionObject> transactionList = transactions.get(current);

        if (transactionList == null) {
          // Unfortunately without Java 9 one can't use List.of(...) :(
          transactionList = new ArrayList<>();
          transactionList.add(transaction);

          // An empty subject might have been created already
          if (transactionsSubject.containsKey(current)) {
            // Updating subject
            transactionsSubject.get(current).onNext(transactionList);
          } else {
            // Creating new subject
            transactionsSubject.put(current, BehaviorSubject.createDefault(transactionList));
          }
        } else if (!transactionList.contains(transaction)) {
          transactionList.add(transaction);
          transactionsSubject.get(current).onNext(transactionList);
        }
        transactions.put(current, new ArrayList<>(transactionList));
      }
    }

    public Observable<List<TransactionObject>> getTransactionsObservable(PublicKey user) {
      // Load from the db the digital cash state for a lao
      loadLaoState();
      return transactionsSubject.computeIfAbsent(
          user, newUser -> BehaviorSubject.createDefault(new ArrayList<>()));
    }

    public List<TransactionObject> getTransactions(PublicKey user) {
      List<TransactionObject> transactionList = transactions.get(user);
      return transactionList == null ? null : new ArrayList<>(transactionList);
    }

    private List<PublicKey> getReceiversTransaction(TransactionObject transaction) {
      // For each output, we get the hash of the public key. Then for each hash we return the
      // preimage (the public key)
      return transaction.getOutputs().stream()
          .map(OutputObject::getPubKeyHash)
          .map(
              hash -> {
                PublicKey key = hashDictionary.get(hash);
                if (key == null) {
                  throw new IllegalStateException("The hash is not in dictionary of known hashes");
                }
                return key;
              })
          .collect(Collectors.toList());
    }

    public long getUserBalance(PublicKey user) {
      List<TransactionObject> transactionList = transactions.get(user);
      return transactionList == null
          ? 0
          : transactionList.stream()
              .mapToLong(transaction -> transaction.getSumForUser(user))
              .sum();
    }

    /**
     * This function loads the state of the digital cash for a given lao, retrieving the list of
     * transactions and the hash dictionary. This is done only once per LAO and when the user
     * accesses such LAO's digital cash section.
     */
    private void loadLaoState() {
      if (alreadyRetrieved) {
        return;
      }
      repository.disposables.add(
          repository
              .hashDao
              .getDictionaryByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  hashEntities -> {
                    // Firstly load the dictionary
                    hashEntities.forEach(
                        hashEntity ->
                            hashDictionary.put(hashEntity.getHash(), hashEntity.getPublicKey()));
                    // Then load the transactions
                    repository.disposables.add(
                        repository
                            .transactionDao
                            .getTransactionsByLaoId(laoId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                transactionObjects ->
                                    transactionObjects.forEach(
                                        transactionObject -> {
                                          try {
                                            updateTransactions(transactionObject, false);
                                          } catch (NoRollCallException e) {
                                            // This exception can't ever be thrown, as if the
                                            // transactions are in the db,
                                            // then they have a valid public key
                                            throw new RuntimeException(e);
                                          }
                                        }),
                                err ->
                                    Timber.tag(TAG)
                                        .e(err, "No transaction to load for lao %s", laoId)));
                  },
                  err -> Timber.tag(TAG).e(err, "No hash dictionary to load for lao %s", laoId)));
      alreadyRetrieved = true;
    }
  }
}
