package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static com.github.dedis.popstellar.testutils.ObservableUtils.assertCurrentValueIs;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DigitalCashRepositoryTest {
  private static final KeyPair ORGANIZER = Base64DataUtils.generateKeyPair();
  private static final KeyPair USER1 = Base64DataUtils.generateKeyPair();
  private static final PublicKey USER1_PK = USER1.getPublicKey();
  private static final KeyPair USER2 = Base64DataUtils.generateKeyPair();
  private static final PublicKey USER2_PK = USER2.getPublicKey();
  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 1000, "LAO");
  private static final int DEFAULT_VALUE = Integer.MAX_VALUE;
  private static DigitalCashRepository repo;

  @Before
  public void initializeRepo() {
    repo = new DigitalCashRepository();
  }

  @Test
  public void updatingRepoWithoutInitializationThrowsError() throws GeneralSecurityException {
    // In this test the actual content of the transaction do not matter
    TransactionObject transactionObject = getValidTransactionBuilder("random id", USER1).build();
    assertThrows(
        NoRollCallException.class, () -> repo.updateTransactions(LAO_ID, transactionObject));
  }

  @Test
  public void getTransactionsTest() throws GeneralSecurityException, NoRollCallException {
    // In this test the actual content of the transaction do not matter, only that it involves
    // user 1
    TransactionObject transactionObject = getValidTransactionBuilder("random id", USER1).build();
    repo.initializeDigitalCash(LAO_ID, Collections.singletonList(USER1_PK));
    repo.updateTransactions(LAO_ID, transactionObject);
    List<TransactionObject> transactionObjectList = repo.getTransactions(LAO_ID, USER1_PK);
    assertEquals(1, transactionObjectList.size());
    assertEquals(transactionObject, transactionObjectList.get(0));
  }

  @Test
  public void getTransactionsObservableTest() throws GeneralSecurityException, NoRollCallException {
    // In this test the actual content of the transaction do not matter, only whether they concern
    // user 1 or user 2

    repo.initializeDigitalCash(LAO_ID, Arrays.asList(USER1_PK, USER2_PK));
    ArrayList<TransactionObject> transactionList = new ArrayList<>();
    TestObserver<List<TransactionObject>> observer =
        repo.getTransactionsObservable(LAO_ID, USER1_PK).test();

    // assert the observable is currently an empty list
    assertCurrentValueIs(observer, emptyList());

    TransactionObject transactionObject = getValidTransactionBuilder("random id", USER1).build();
    transactionList.add(transactionObject);
    repo.updateTransactions(LAO_ID, transactionObject);

    // assert the observable is currently the list with the single transaction
    assertCurrentValueIs(observer, transactionList);

    TransactionObject transactionObject2 = getValidTransactionBuilder("random id 2", USER1).build();
    transactionList.add(transactionObject2);
    repo.updateTransactions(LAO_ID, transactionObject2);

    // assert the observable is currently the list with the 2 elements
    assertCurrentValueIs(observer, transactionList);

    TransactionObject transactionObjectAttendee = getValidTransactionBuilder("id3", USER2).build();
    repo.updateTransactions(LAO_ID, transactionObjectAttendee);
    observer = repo.getTransactionsObservable(LAO_ID, USER2_PK).test();

    // assert that the transaction from the new user is in the observable
    assertCurrentValueIs(observer, Collections.singletonList(transactionObjectAttendee));
  }

  @Test
  public void simpleIssuanceBalanceTest() throws GeneralSecurityException, NoRollCallException {
    int issuanceAmount = 100_000;
    TransactionObject issuance =
        getValidTransactionBuilder(
                "any", ORGANIZER, Arrays.asList(USER1_PK, USER2_PK), true, issuanceAmount)
            .build();
    repo.initializeDigitalCash(LAO_ID, Arrays.asList(USER1_PK, USER2_PK));
    repo.updateTransactions(LAO_ID, issuance);
    assertEquals(issuanceAmount, repo.getUserBalance(LAO_ID, USER1_PK));
    assertEquals(issuanceAmount, repo.getUserBalance(LAO_ID, USER2_PK));

    int paymentAmount = 500;

    TransactionObject payment =
        getValidTransactionBuilder(
                "anything", USER1, Collections.singletonList(USER2_PK), false, paymentAmount)
            .build();
    repo.updateTransactions(LAO_ID, payment);
    assertEquals(2, repo.getTransactions(LAO_ID, USER2_PK).size());
    assertEquals(2, repo.getTransactions(LAO_ID, USER1_PK).size());

    assertEquals(issuanceAmount + paymentAmount, repo.getUserBalance(LAO_ID, USER2_PK));
    assertEquals(issuanceAmount - paymentAmount, repo.getUserBalance(LAO_ID, USER1_PK));
  }

  private TransactionObjectBuilder getValidTransactionBuilder(String transactionId, KeyPair sender)
      throws GeneralSecurityException {
    TransactionObjectBuilder builder = new TransactionObjectBuilder();

    PublicKey senderPublicKey = sender.getPublicKey();
    String type = "P2PKH";
    int txOutIndex = 0;
    String txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    ScriptInputObject scriptTxIn;
    String sig = sender.sign(senderPublicKey).getEncoded();
    scriptTxIn = new ScriptInputObject(type, senderPublicKey, new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    builder.setInputs(listInput);

    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    builder.setChannel(channel);

    String pubKeyHash = senderPublicKey.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    OutputObject output = new OutputObject(DEFAULT_VALUE, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    builder.setOutputs(listOutput);

    builder.setLockTime(0);
    builder.setVersion(0);
    builder.setTransactionId(transactionId);
    return builder;
  }

  private TransactionObjectBuilder getValidTransactionBuilder(
      String transactionId,
      KeyPair sender,
      List<PublicKey> recipients,
      boolean isIssuance,
      int amount)
      throws GeneralSecurityException {
    TransactionObjectBuilder builder = new TransactionObjectBuilder();

    PublicKey senderPublicKey = sender.getPublicKey();
    String type = "P2PKH";
    int txOutIndex = 0;
    String txOutHash =
        isIssuance
            ? TransactionObject.TX_OUT_HASH_COINBASE
            : "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    ScriptInputObject scriptTxIn;
    String sig = sender.sign(senderPublicKey).getEncoded();
    scriptTxIn = new ScriptInputObject(type, senderPublicKey, new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    builder.setInputs(listInput);

    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    builder.setChannel(channel);

    List<OutputObject> outputObjects =
        recipients.stream()
            .map(
                recipient -> {
                  String pubKeyHash = recipient.computeHash();
                  ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
                  return new OutputObject(amount, scriptTxOut);
                })
            .collect(Collectors.toList());
    if (!isIssuance) {
      outputObjects.add(
          new OutputObject(
              repo.getUserBalance(LAO_ID, senderPublicKey) - amount,
              new ScriptOutputObject(type, senderPublicKey.computeHash())));
    }
    builder.setOutputs(outputObjects);

    builder.setLockTime(0);
    builder.setVersion(0);
    builder.setTransactionId(transactionId);
    return builder;
  }
}
