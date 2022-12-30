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

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static com.github.dedis.popstellar.testutils.ObservableUtils.assertCurrentValueIs;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DigitalCashRepositoryTest {
  private static final KeyPair ORGANIZER = Base64DataUtils.generateKeyPair();
  private static final PublicKey ORGANIZER_PK = ORGANIZER.getPublicKey();
  private static final KeyPair ATTENDEE = Base64DataUtils.generateKeyPair();
  private static final PublicKey ATTENDEE_PK = ATTENDEE.getPublicKey();
  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 1000, "LAO");
  private static DigitalCashRepository repo;

  @Before
  public void initializeRepo() {
    repo = new DigitalCashRepository();
  }

  @Test
  public void updatingRepoWithoutInitializationThrowsError() throws GeneralSecurityException {
    TransactionObject transactionObject =
        getValidTransactionBuilder("random id", ORGANIZER).build();
    assertThrows(
        NoRollCallException.class, () -> repo.updateTransactions(LAO_ID, transactionObject));
  }

  @Test
  public void getTransactionsTest() throws GeneralSecurityException, NoRollCallException {
    TransactionObject transactionObject =
        getValidTransactionBuilder("random id", ORGANIZER).build();
    repo.initializeDigitalCash(LAO_ID, Collections.singletonList(ORGANIZER_PK));
    repo.updateTransactions(LAO_ID, transactionObject);
    List<TransactionObject> transactionObjectList = repo.getTransactions(LAO_ID, ORGANIZER_PK);
    assertEquals(1, transactionObjectList.size());
    assertEquals(transactionObject, transactionObjectList.get(0));
  }

  @Test
  public void getTransactionsObservableTest() throws GeneralSecurityException, NoRollCallException {
    repo.initializeDigitalCash(LAO_ID, Arrays.asList(ORGANIZER_PK, ATTENDEE_PK));
    ArrayList<TransactionObject> transactionList = new ArrayList<>();
    TestObserver<List<TransactionObject>> observer =
        repo.getTransactionsObservable(LAO_ID, ORGANIZER_PK).test();

    // assert the observable is currently an empty list
    assertCurrentValueIs(observer, emptyList());

    TransactionObject transactionObject =
        getValidTransactionBuilder("random id", ORGANIZER).build();
    transactionList.add(transactionObject);
    repo.updateTransactions(LAO_ID, transactionObject);

    // assert the observable is currently the list with the single transaction
    assertCurrentValueIs(observer, transactionList);

    TransactionObject transactionObject2 =
        getValidTransactionBuilder("random id 2", ORGANIZER).build();
    transactionList.add(transactionObject2);
    repo.updateTransactions(LAO_ID, transactionObject2);

    // assert the observable is currently the list with the 2 elements
    assertCurrentValueIs(observer, transactionList);

    TransactionObject transactionObjectAttendee =
        getValidTransactionBuilder("id3", ATTENDEE).build();
    repo.updateTransactions(LAO_ID, transactionObjectAttendee);
    observer = repo.getTransactionsObservable(LAO_ID, ATTENDEE_PK).test();
    assertCurrentValueIs(observer, Collections.singletonList(transactionObjectAttendee));
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
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    builder.setOutputs(listOutput);

    builder.setLockTime(0);
    builder.setVersion(0);
    builder.setTransactionId(transactionId);
    return builder;
  }
}
