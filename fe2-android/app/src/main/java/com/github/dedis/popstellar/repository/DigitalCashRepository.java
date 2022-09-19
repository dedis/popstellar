package com.github.dedis.popstellar.repository;

import android.util.Log;

import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.*;

import javax.inject.Inject;

public class DigitalCashRepository {

  public static final String TAG = DigitalCashRepository.class.getSimpleName();

  private final Map<String, LaoTransactions> transactionsByLao = new HashMap<>();
  private final Map<String, LaoTransactions> transactionHistoryByLao = new HashMap<>();
  private final Map<String, PublicKey> publicKeyByHash = new HashMap<>();

  @Inject
  public DigitalCashRepository() {
    // Constructor required by Hilt
  }

  private synchronized LaoTransactions getLaoTransactions(String laoId) {
    // Create the lao chirps object if it is not present yet
    return transactionsByLao.computeIfAbsent(laoId, id -> new LaoTransactions());
  }

  private synchronized LaoTransactions getLaoTransactionHistory(String laoId){
    // Create the lao chirps object if it is not present yet
    return transactionHistoryByLao.computeIfAbsent(laoId, id -> new LaoTransactions());
  }

  private synchronized TransactionObject getTransaction(String laoId, MessageID id){
    return getLaoTransactions(laoId)
  }

  public void addTransaction(String laoId, TransactionObject transaction) {
     Log.d(TAG, )
  }

  private static final class LaoTransactions {
    private final Map<PublicKey, Set<TransactionObject>> transactionsByUser = new HashMap<>();

    public synchronized void add(PublicKey user, TransactionObject transaction){
      transactionsByUser.computeIfAbsent(user)
    }
  }
}
