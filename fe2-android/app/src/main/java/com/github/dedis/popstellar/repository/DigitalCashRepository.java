package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.OutputObject;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.digitalcash.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
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

  /**
   * Get the list of transactions that a given user has performed.
   *
   * @param laoId of the lao the transactions are part of
   * @param user public key
   * @return a list of transaction objects
   */
  public List<TransactionObject> getTransactions(String laoId, PublicKey user) {
    return getLaoTransactions(laoId).getTransactions(user);
  }

  /**
   * This returns an observable on the list of transactions that a given user performs.
   *
   * @param laoId of the lao the transactions are part of
   * @param user public key
   * @return an observable that is notified whenever a new transaction is added/updated
   */
  public Observable<List<TransactionObject>> getTransactionsObservable(
      String laoId, PublicKey user) {
    return getLaoTransactions(laoId).getTransactionsObservable(user);
  }

  /**
   * The function returns the user balance in miniLAOs based on the transactions he's involved into.
   *
   * @param laoId of the lao the transactions are part of
   * @param user public key
   * @return miniLAOs amount as long
   */
  public long getUserBalance(String laoId, PublicKey user) {
    return getLaoTransactions(laoId).getUserBalance(user);
  }

  /**
   * This updates and persist a transaction in a digital cash state of a lao.
   *
   * @param laoId of the lao the transaction is part of
   * @param transaction digital cash transaction to add
   * @throws NoRollCallException if no roll call attendees is found
   */
  public void updateTransactions(String laoId, TransactionObject transaction)
      throws NoRollCallException {
    Timber.tag(TAG).d("updating transactions on Lao %s and transaction %s", laoId, transaction);
    getLaoTransactions(laoId).updateTransactions(transaction, true);
  }

  /**
   * This function initializes the digital cash for a given lao. This happens after a rollcall is
   * closed to reset the previous state and add the current attendees.
   *
   * @param laoId of the lao whose digital cash we are looking for
   * @param attendees set of public keys of the attendees the the last closed roll call
   */
  public void initializeDigitalCash(String laoId, List<PublicKey> attendees) {
    getLaoTransactions(laoId).initializeDigitalCash(attendees);
  }

  @NonNull
  private synchronized LaoTransactions getLaoTransactions(String laoId) {
    // Create the lao transactions object if it is not present yet
    return transactionsByLao.computeIfAbsent(laoId, lao -> new LaoTransactions(laoId, this));
  }

  private static final class LaoTransactions {

    private final String laoId;
    private final DigitalCashRepository repository;

    /**
     * Thread-safe map that maps the users' public keys to a thread-safe list of transactions which
     * they have been involved into (either senders or receivers)
     */
    private final ConcurrentHashMap<PublicKey, ConcurrentLinkedQueue<TransactionObject>>
        transactions = new ConcurrentHashMap<>();

    /** Thread-safe map that maps the users' public keys to the observable on their transactions */
    private final ConcurrentHashMap<PublicKey, Subject<List<TransactionObject>>>
        transactionsSubject = new ConcurrentHashMap<>();

    /** Thread-safe dictionary to maps the hash to the user's public key */
    private final ConcurrentHashMap<String, PublicKey> hashDictionary = new ConcurrentHashMap<>();

    public LaoTransactions(String laoId, DigitalCashRepository repository) {
      this.laoId = laoId;
      this.repository = repository;
      loadLaoState();
    }

    /**
     * This resets the digital cash state. This function is called each time a rc is closed.
     *
     * @param attendees the attendees of the closed roll call
     */
    public void initializeDigitalCash(List<PublicKey> attendees) {
      Timber.tag(TAG).d("initializing digital cash with attendees %s", attendees);

      // Clear the transactions on the database for the given lao
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

      // Clear the memory
      hashDictionary.clear();
      transactions.clear();
      transactionsSubject.clear();
      transactionsSubject.values().forEach(observer -> observer.toSerialized().onComplete());

      // Reset the hash dictionary
      List<HashEntity> hashEntities = new ArrayList<>();
      attendees.forEach(
          publicKey -> {
            String hash = publicKey.computeHash();
            hashDictionary.put(hash, publicKey);
            // Save the mapping in a list
            hashEntities.add(new HashEntity(hash, laoId, publicKey));
          });

      // Save all the entries at once to minimize I/O accesses
      // Ensure to delete before adding the new entries
      repository.disposables.add(
          repository
              .hashDao
              .deleteByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () -> {
                    Timber.tag(TAG).d("Cleared the hash dictionary in the db for lao %s", laoId);
                    // After having it deleted, save the new entities
                    repository.disposables.add(
                        repository
                            .hashDao
                            .insertAll(hashEntities)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                () ->
                                    Timber.tag(TAG)
                                        .d(
                                            "Successfully persisted hash dictionary for lao %s",
                                            laoId),
                                err ->
                                    Timber.tag(TAG)
                                        .e(
                                            err,
                                            "Error in persisting the hash dictionary for lao %s",
                                            laoId)));
                  },
                  err ->
                      Timber.tag(TAG)
                          .e(err, "Error in clearing the hash dictionary for lao %s", laoId)));
    }

    public void updateTransactions(TransactionObject transaction, boolean toBeStored)
        throws NoRollCallException {
      if (hashDictionary.isEmpty()) {
        throw new NoRollCallException("No roll call attendees could be found");
      }

      for (PublicKey current : getReceiversTransaction(transaction)) {
        ConcurrentLinkedQueue<TransactionObject> transactionList = transactions.get(current);

        if (transactionList == null) {
          transactionList = new ConcurrentLinkedQueue<>();
          transactionList.add(transaction);

          // An empty subject might have been created already
          if (transactionsSubject.containsKey(current)) {
            // Updating subject
            Objects.requireNonNull(transactionsSubject.get(current))
                .toSerialized()
                .onNext(new ArrayList<>(transactionList));
          } else {
            // Creating new subject
            transactionsSubject.put(
                current, BehaviorSubject.createDefault(new ArrayList<>(transactionList)));
          }

          // Add it to the map
          transactions.put(current, transactionList);
        } else if (!transactionList.contains(transaction)) {
          transactionList.add(transaction);
          Objects.requireNonNull(transactionsSubject.get(current))
              .toSerialized()
              .onNext(new ArrayList<>(transactionList));
        }
      }

      // Store the transaction in the db if the flag is true
      if (toBeStored) {
        TransactionEntity transactionEntity = new TransactionEntity(laoId, transaction);
        repository.disposables.add(
            repository
                .transactionDao
                .insert(transactionEntity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () ->
                        Timber.tag(TAG)
                            .d(
                                "Successfully persisted transaction %s",
                                transaction.getTransactionId()),
                    err ->
                        Timber.tag(TAG)
                            .e(
                                err,
                                "Error in persisting the transaction %s",
                                transaction.getTransactionId())));
      }
    }

    public Observable<List<TransactionObject>> getTransactionsObservable(PublicKey user) {
      return transactionsSubject.computeIfAbsent(
          user, newUser -> BehaviorSubject.createDefault(new ArrayList<>()));
    }

    public List<TransactionObject> getTransactions(PublicKey user) {
      ConcurrentLinkedQueue<TransactionObject> transactionList = transactions.get(user);
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
      ConcurrentLinkedQueue<TransactionObject> transactionList = transactions.get(user);
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
                    Timber.tag(TAG).d("Retrieved the hash dictionary from db");
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
                                          Timber.tag(TAG)
                                              .d(
                                                  "Retrieved transaction %s from db",
                                                  transactionObject.getTransactionId());
                                          try {
                                            updateTransactions(transactionObject, false);
                                          } catch (NoRollCallException e) {
                                            // This exception can't ever be thrown, as if the
                                            // transactions are in the db,
                                            // then they have a valid public key associated
                                          }
                                        }),
                                err ->
                                    Timber.tag(TAG)
                                        .e(err, "No transaction to load for lao %s", laoId)));
                  },
                  err -> Timber.tag(TAG).e(err, "No hash dictionary to load for lao %s", laoId)));
    }
  }
}
