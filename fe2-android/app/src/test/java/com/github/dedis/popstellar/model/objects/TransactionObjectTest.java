package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptInput;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptOutput;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.utility.security.Hash;
import com.google.gson.JsonParseException;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
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

  // test thrown null List<PublicKey> getReceiversTransaction(Map<String, PublicKey> mapHashKey)
  // test thrown null Map<PublicKey, Long> getReceiversTransactionMap(Map<String, PublicKey>
  // mapHashKey)
  @Test
  public void getReceiversTransactionTestNull() {
    /*public List<PublicKey> getReceiversTransaction(Map<String, PublicKey> mapHashKey) {
      Iterator<String> receiverHashIte = getReceiversHashTransaction().iterator();
      List<PublicKey> receivers = new ArrayList<>();
      while (receiverHashIte.hasNext()){
        PublicKey pub = mapHashKey.getOrDefault(receiverHashIte.next(),null);
        if (pub == null) {
          throw new IllegalArgumentException("The hash correspond to no key in the dictionary");
        }
        receivers.add(pub);
      }
      return receivers;
    }*/

    KeyPair senderKey1 = generateKeyPair();
    PublicKey sender1 = senderKey1.getPublicKey();
    PublicKey sender2 = null;
    String type = "P2PKH";
    String pubkeyhash1 = sender1.computeHash();
    String pubkeyhash2 = "none";
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubkeyhash1);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    Map<String, PublicKey> mapHash = Collections.singletonMap(pubkeyhash2, sender2);
    transactionObject.setOutputs(listOutput);
    assertThrows(
        IllegalArgumentException.class, () -> transactionObject.getReceiversTransaction(mapHash));
    assertThrows(
        IllegalArgumentException.class,
        () -> transactionObject.getReceiversTransactionMap(mapHash));
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

  // test Map<PublicKey, Long> getReceiversTransactionMap(Map<String, PublicKey> mapHashKey)
  @Test
  public void getReceiversTransactionMapTest() {
    /*  public Map<PublicKey, Long> getReceiversTransactionMap(Map<String, PublicKey> mapHashKey) {
      Iterator<String> receiverHashIte = getReceiversHashTransaction().iterator();
      Map<PublicKey, Long> receivers = new HashMap<>();
      while (receiverHashIte.hasNext()) {
        PublicKey pub = mapHashKey.getOrDefault(receiverHashIte.next(), null);
        if (pub == null) {
          throw new IllegalArgumentException("The hash correspond to no key in the dictionary");
        }
        receivers.putIfAbsent(pub, this.getMiniLaoPerReceiver(pub));
      }
      return receivers;
    }*/
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();
    String type = "P2PKH";
    String pubkeyhash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubkeyhash);
    long value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    Map<String, PublicKey> mapHash = Collections.singletonMap(pubkeyhash, sender);
    Map<PublicKey, Long> mapValue = Collections.singletonMap(sender, value);
    transactionObject.setOutputs(listOutput);
    assertEquals(mapValue, transactionObject.getReceiversTransactionMap(mapHash));
  }

  // test boolean isReceiver(PublicKey publicKey)
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

  // test boolean is Sender(PublicKey publicKey)
  @Test
  public void getIsSenderTest() {
    KeyPair senderKey = generateKeyPair();
    PublicKey sender = senderKey.getPublicKey();
    String type = "P2PKH";
    ScriptInputObject scriptTxInput = new ScriptInputObject(type, sender, new Signature("qqchose"));
    InputObject input = new InputObject(Hash.hash("none"), 0, scriptTxInput);
    List<InputObject> listInput = Collections.singletonList(input);
    transactionObject.setInputs(listInput);
    assertEquals(true, transactionObject.isSender(sender));
  }

  // test String compute_sig_outputs_inputs(KeyPair keyPair)
  // @Test
  // public void computeSigOutputsInputs()

  // test int get_miniLao_per_receiver(PublicKey receiver)
  // send to your self still work
  // test long getMiniLaoPerReceiverFirst(PublicKey receiver)
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
    KeyPair senderKey1 = generateKeyPair();
    PublicKey sender1 = senderKey1.getPublicKey();
    assertThrows(
        IllegalArgumentException.class, () -> transactionObject.getMiniLaoPerReceiver(sender1));
    assertEquals(value, transactionObject.getMiniLaoPerReceiverFirst(sender));
    assertThrows(
        IllegalArgumentException.class,
        () -> transactionObject.getMiniLaoPerReceiverFirst(sender1));
  }

  // test getMiniLaoPerReceiverSetTransaction
  @Test
  public void getMiniLaoPerReceiverSetTransactionTest() {
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
        32,
        TransactionObject.getMiniLaoPerReceiverSetTransaction(
            Collections.singletonList(transactionObject), sender));
    List<TransactionObject> list = new ArrayList<>();
    list.add(transactionObject);
    list.add(transactionObject);
    assertEquals(64, TransactionObject.getMiniLaoPerReceiverSetTransaction(list, sender));
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

  @Test
  public void computeIdTest() {
    String path = "protocol/examples/messageData/coin/post_transaction_coinbase.json";
    String validJson = JsonTestUtils.loadFile(path);
    PostTransactionCoin postTransactionModel = (PostTransactionCoin) JsonTestUtils.parse(validJson);
    Transaction transactionModel = postTransactionModel.getTransaction();

    TransactionObject transactionObject = new TransactionObject();
    transactionObject.setLockTime(transactionModel.getLockTime());
    transactionObject.setVersion(transactionModel.getVersion());

    List<InputObject> inpObj = new ArrayList<>();
    List<OutputObject> outObj = new ArrayList<>();

    for (Input i: transactionModel.getInputs()) {
      ScriptInput scriptInput = i.getScript();
      inpObj.add(new InputObject(i.getTxOutHash(), i.getTxOutIndex(), new ScriptInputObject(scriptInput.getType(), scriptInput.getPubkey(), scriptInput.getSig())));
    }

    for (Output o: transactionModel.getOutputs()) {
      ScriptOutput scriptOutput = o.getScript();
      outObj.add(new OutputObject(o.getValue(), new ScriptOutputObject(scriptOutput.getType(), scriptOutput.getPubkeyHash())));
    }

    transactionObject.setInputs(inpObj);
    transactionObject.setOutputs(outObj);

    assertEquals(true, transactionObject.isCoinBaseTransaction());
    assertEquals(postTransactionModel.getTransactionId(), transactionObject.computeId());
  }
}
