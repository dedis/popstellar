package com.github.dedis.popstellar.repository;

import android.util.Log;

import com.github.dedis.popstellar.model.objects.OutputObject;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

@Singleton
public class DigitalCashRepository {

  public static final String TAG = DigitalCashRepository.class.getSimpleName();

  private final Map<String, LaoTransactions> transactionsByLao = new HashMap<>();

  @Inject
  public DigitalCashRepository() {
    // Constructor required by Hilt
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
    Log.d(TAG, "updating transactions on Lao " + laoId + " and transaction " + transaction);
    getLaoTransactions(laoId).updateTransactions(transaction);
  }

  public void initializeDigitalCash(String laoId, List<PublicKey> attendees) {
    getLaoTransactions(laoId).initializeDigitalCash(attendees);
  }

  private synchronized LaoTransactions getLaoTransactions(String laoId) {
    // Create the lao transactions object if it is not present yet
    return transactionsByLao.computeIfAbsent(laoId, lao -> new LaoTransactions());
  }

  private static final class LaoTransactions {

    private final Map<PublicKey, List<TransactionObject>> transactions = new HashMap<>();
    private final Map<PublicKey, Subject<List<TransactionObject>>> transactionsSubject =
        new HashMap<>();
    private final Map<String, PublicKey> hashDictionary = new HashMap<>();

    /**
     * This resets the digital cash state. This should be called each time a rc is closed
     *
     * @param attendees the attendees of the closed roll call
     */
    public synchronized void initializeDigitalCash(List<PublicKey> attendees) {
      transactions.clear();
      hashDictionary.clear();
      transactionsSubject.values().forEach(Observer::onComplete);
      transactionsSubject.clear();
      attendees.forEach(publicKey -> hashDictionary.put(publicKey.computeHash(), publicKey));
      Log.d(TAG, "initializing digital cash with attendees " + attendees);
    }

    public synchronized void updateTransactions(TransactionObject transaction)
        throws NoRollCallException {
      if (hashDictionary.isEmpty()) {
        throw new NoRollCallException("No roll call attendees could be found");
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
  }
}
