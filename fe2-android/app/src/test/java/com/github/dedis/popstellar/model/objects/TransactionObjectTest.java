package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TransactionObjectTest {

  private static TransactionObject transactionObject = new TransactionObject();

  @Test
  public void setAndGetChannelTest() {
    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    transactionObject.setChannel(channel);
    assertEquals(channel, transactionObject.getChannel());
  }

  // test get Inputs
  @Test
  public void setAndGetInputsTest() throws GeneralSecurityException {
    int txOutIndex = 0;
    String txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();

    String type = "P2PKH";
    String pubKey = sender.getEncoded();
    ScriptInputObject scriptTxIn;
    String sig = senderKey.sign(sender).getEncoded();
    scriptTxIn = new ScriptInputObject(type, new PublicKey(pubKey), new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    transactionObject.setInputs(listInput);
    assertEquals(listInput, transactionObject.getInputs());
  }

  // test get Outputs
  @Test
  public void setAndGetOutputsTest() {
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();
    String type = "P2PKH";
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    transactionObject.setOutputs(listOutput);
    assertEquals(listOutput, transactionObject.getOutputs());
  }

  // test get Locktime
  @Test
  public void setGetLockTimeTest() {
    long locktime = 0;
    transactionObject.setLockTime(locktime);
    assertEquals(locktime, transactionObject.getLockTime());
  }

  // test get version
  @Test
  public void setGetVersionTest() {
    int version = 0;
    transactionObject.setVersion(version);
    assertEquals(version, transactionObject.getVersion());
  }

  @Test
  public void getSendersTransactionTest() {
    int txOutIndex = 0;
    String txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();

    String type = "P2PKH";
    String pubKey = sender.getEncoded();
    ScriptInputObject scriptTxIn;
    String sig = "dhfqkdfhqu";
    scriptTxIn = new ScriptInputObject(type, new PublicKey(pubKey), new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    transactionObject.setInputs(listInput);
    assertEquals(Collections.singletonList(sender), transactionObject.getSendersTransaction());
  }

  @Test
  public void getReceiversHashTransactionTest() {
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();
    String type = "P2PKH";
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    transactionObject.setOutputs(listOutput);
    assertEquals(
        Collections.singletonList(pubKeyHash), transactionObject.getReceiversHashTransaction());
  }

  // test List<PublicKey> get_receivers_transaction(Map<String, PublicKey> map_hash_key)
  @Test
  public void getReceiversTransactionTest() {
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();
    String type = "P2PKH";
    String pubkeyhash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubkeyhash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    Map<String, PublicKey> mapHash = Collections.singletonMap(pubkeyhash, sender);
    transactionObject.setOutputs(listOutput);
    assertEquals(
        Collections.singletonList(sender), transactionObject.getReceiversTransaction(mapHash));
  }

  // test boolean is_receiver(PublicKey publicKey)
  @Test
  public void getIsReceiverTest() {
    // SENDER
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();
    String type = "P2PKH";
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    transactionObject.setOutputs(listOutput);

    assertEquals(
        Collections.singletonList(pubKeyHash), transactionObject.getReceiversHashTransaction());
    // DUMMY SENDER
    KeyPair senderDummyKey = generateKeyPair();
    PublicKey senderDummy = senderDummyKey.getPublicKey();
    assertNotEquals(
        Collections.singletonList(senderDummy.computeHash()),
        transactionObject.getReceiversHashTransaction());
  }

  // test String compute_sig_outputs_inputs(KeyPair keyPair)
  // @Test
  // public void computeSigOutputsInputs()

  // test int get_miniLao_per_receiver(PublicKey receiver)
  @Test
  public void getMiniLaoPerReceiverTest() {
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();

    String type = "P2PKH";
    // RECEIVER
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    transactionObject.setOutputs(listOutput);
    assertEquals(value, transactionObject.getMiniLaoPerReceiver(sender));
  }

  // test int get_index_transaction(PublicKey publicKey)
  @Test
  public void getIndexTransactionTest() {
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();

    String type = "P2PKH";
    // RECEIVER
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    transactionObject.setOutputs(listOutput);
    assertEquals(0, transactionObject.getIndexTransaction(sender));
  }
}
