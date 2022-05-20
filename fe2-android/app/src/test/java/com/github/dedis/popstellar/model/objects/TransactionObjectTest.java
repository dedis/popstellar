package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TransactionObjectTest {

  private static TransactionObject transaction_object = new TransactionObject();

  @Test
  public void setAndGetChannelTest() {
    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    transaction_object.setChannel(channel);
    assertEquals(channel, transaction_object.getChannel());
  }

  // test get Inputs
  @Test
  public void setAndGetInputsTest() throws GeneralSecurityException {
    int TX_OUT_INDEX = 0;
    String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();

    String TYPE = "P2PKH";
    String PUBKEY = SENDER.getEncoded();
    ScriptInputObject SCRIPTTXIN;
    String SIG = SENDER_KEY.sign(SENDER).getEncoded();
    SCRIPTTXIN = new ScriptInputObject(TYPE, PUBKEY, SIG);
    InputObject INPUT = new InputObject(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);
    List<InputObject> list_input = Collections.singletonList(INPUT);
    transaction_object.setInputs(list_input);
    assertEquals(list_input, transaction_object.getInputs());
  }

  // test get Outputs
  @Test
  public void setAndGetOutputsTest() {
    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();
    String TYPE = "P2PKH";
    String PUBKEYHASH = SENDER.computeHash();
    ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);
    int VALUE = 32;
    OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);
    List<OutputObject> list_output = Collections.singletonList(OUTPUT);
    transaction_object.setOutputs(list_output);
    assertEquals(list_output, transaction_object.getOutputs());
  }

  // test get Locktime
  @Test
  public void setGetLockTimeTest() {
    long LOCKTIME = 0;
    transaction_object.setLockTime(LOCKTIME);
    assertEquals(LOCKTIME, transaction_object.getLockTime());
  }

  // test get version
  @Test
  public void setGetVersionTest() {
    int VERSION = 0;
    transaction_object.setVersion(VERSION);
    assertEquals(VERSION, transaction_object.getVersion());
  }

  @Test
  public void getSendersTransactionTest() {
    int TX_OUT_INDEX = 0;
    String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();

    String TYPE = "P2PKH";
    String PUBKEY = SENDER.getEncoded();
    ScriptInputObject SCRIPTTXIN;
    String SIG = "dhfqkdfhqu";
    SCRIPTTXIN = new ScriptInputObject(TYPE, PUBKEY, SIG);
    InputObject INPUT = new InputObject(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);
    List<InputObject> list_input = Collections.singletonList(INPUT);
    transaction_object.setInputs(list_input);
    assertEquals(Collections.singletonList(SENDER), transaction_object.getSendersTransaction());
  }

  @Test
  public void getReceiversHashTransactionTest() {
    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();
    String TYPE = "P2PKH";
    String PUBKEYHASH = SENDER.computeHash();
    ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);
    int VALUE = 32;
    OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);
    List<OutputObject> list_output = Collections.singletonList(OUTPUT);
    transaction_object.setOutputs(list_output);
    assertEquals(
        Collections.singletonList(PUBKEYHASH), transaction_object.getReceiversHashTransaction());
  }

  // test List<PublicKey> get_receivers_transaction(Map<String, PublicKey> map_hash_key)
  @Test
  public void getReceiversTransactionTest() {
    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();
    String TYPE = "P2PKH";
    String PUBKEYHASH = SENDER.computeHash();
    ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);
    int VALUE = 32;
    OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);
    List<OutputObject> list_output = Collections.singletonList(OUTPUT);
    Map<String, PublicKey> map_hash = Collections.singletonMap(PUBKEYHASH, SENDER);
    transaction_object.setOutputs(list_output);
    assertEquals(
        Collections.singletonList(SENDER), transaction_object.getReceiversTransaction(map_hash));
  }

  // test boolean is_receiver(PublicKey publicKey)
  @Test
  public void getIsReceiverTest() {
    // SENDER
    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();

    String TYPE = "P2PKH";
    String PUBKEYHASH = SENDER.computeHash();
    ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);
    int VALUE = 32;
    OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);
    List<OutputObject> list_output = Collections.singletonList(OUTPUT);
    transaction_object.setOutputs(list_output);

    assertEquals(
        Collections.singletonList(PUBKEYHASH), transaction_object.getReceiversHashTransaction());
    // DUMMY SENDER
    KeyPair SENDER_dummy_KEY = generateKeyPair();
    PublicKey SENDER_dummy = SENDER_dummy_KEY.getPublicKey();
    assertNotEquals(
        Collections.singletonList(SENDER_dummy.computeHash()),
        transaction_object.getReceiversHashTransaction());
  }

  // test String compute_sig_outputs_inputs(KeyPair keyPair)
  // @Test
  // public void computeSigOutputsInputs()

  // test int get_miniLao_per_receiver(PublicKey receiver)
  @Test
  public void getMiniLaoPerReceiverTest() {
    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();

    String TYPE = "P2PKH";
    // RECEIVER
    String PUBKEYHASH = SENDER.computeHash();
    ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);
    int VALUE = 32;
    OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);
    List<OutputObject> list_output = Collections.singletonList(OUTPUT);
    transaction_object.setOutputs(list_output);
    assertEquals(VALUE, transaction_object.getMiniLaoPerReceiver(SENDER));
  }

  // test int get_index_transaction(PublicKey publicKey)
  @Test
  public void getIndexTransactionTest() {
    KeyPair SENDER_KEY = generateKeyPair();
    PublicKey SENDER = SENDER_KEY.getPublicKey();

    String TYPE = "P2PKH";
    // RECEIVER
    String PUBKEYHASH = SENDER.computeHash();
    ScriptOutputObject SCRIPTTXOUT = new ScriptOutputObject(TYPE, PUBKEYHASH);
    int VALUE = 32;
    OutputObject OUTPUT = new OutputObject(VALUE, SCRIPTTXOUT);
    List<OutputObject> list_output = Collections.singletonList(OUTPUT);
    transaction_object.setOutputs(list_output);
    assertEquals(0, transaction_object.getIndexTransaction(SENDER));
  }
}
