package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.objects.digitalcash.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.*;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.*;

public class TransactionObjectTest {
  KeyPair senderKey = generateKeyPair();
  PublicKey sender = senderKey.getPublicKey();

  @Test
  public void getChannelTest() throws GeneralSecurityException {
    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    builder.setChannel(channel);
    assertEquals(channel, builder.build().getChannel());
  }

  // test get Inputs
  @Test
  public void getInputsTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    int txOutIndex = 0;
    String txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";
    String type = "P2PKH";

    ScriptInputObject scriptTxIn;
    String sig = senderKey.sign(sender).getEncoded();
    scriptTxIn = new ScriptInputObject(type, sender, new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    builder.setInputs(listInput);

    assertEquals(listInput, builder.build().getInputs());
  }

  // test get Outputs
  @Test
  public void getOutputsTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    String type = "P2PKH";
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    builder.setOutputs(listOutput);
    assertEquals(listOutput, builder.build().getOutputs());
  }

  @Test
  public void getLockTimeTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    long locktime = 0;
    builder.setLockTime(locktime);
    assertEquals(locktime, builder.build().getLockTime());
  }

  @Test
  public void getVersionTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    int version = 0;
    builder.setVersion(version);
    assertEquals(version, builder.build().getVersion());
  }

  @Test
  public void getSendersTransactionTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    int txOutIndex = 0;
    String txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    String type = "P2PKH";
    String pubKey = sender.getEncoded();
    ScriptInputObject scriptTxIn;
    String sig = "dhfqkdfhqu";
    scriptTxIn = new ScriptInputObject(type, new PublicKey(pubKey), new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    builder.setInputs(listInput);
    assertEquals(Collections.singletonList(sender), builder.build().getSendersTransaction());
  }

  @Test
  public void getReceiversHashTransactionTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();

    String type = "P2PKH";
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    builder.setOutputs(listOutput);
    assertEquals(
        Collections.singletonList(pubKeyHash), builder.build().getReceiversHashTransaction());
  }

  // test thrown null List<PublicKey> getReceiversTransaction(Map<String, PublicKey> mapHashKey)
  // test thrown null Map<PublicKey, Long> getReceiversTransactionMap(Map<String, PublicKey>
  // mapHashKey)
  @Test
  public void getReceiversTransactionTestNull() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
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
    builder.setOutputs(listOutput);
    TransactionObject transactionObject = builder.build();
    assertThrows(
        IllegalArgumentException.class, () -> transactionObject.getReceiversTransaction(mapHash));
  }

  @Test
  public void getReceiversTransactionTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    String type = "P2PKH";
    String pubkeyhash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubkeyhash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    Map<String, PublicKey> mapHash = Collections.singletonMap(pubkeyhash, sender);
    builder.setOutputs(listOutput);
    assertEquals(
        Collections.singletonList(sender), builder.build().getReceiversTransaction(mapHash));
  }

  @Test
  public void getIsReceiverTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();

    String type = "P2PKH";
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    builder.setOutputs(listOutput);
    TransactionObject transactionObject = builder.build();
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
  public void getIsSenderTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();
    String type = "P2PKH";
    ScriptInputObject scriptTxInput = new ScriptInputObject(type, sender, new Signature("qqchose"));
    InputObject input = new InputObject(Hash.hash("none"), 0, scriptTxInput);
    List<InputObject> listInput = Collections.singletonList(input);
    builder.setInputs(listInput);
    assertTrue(builder.build().isSender(sender));
  }

  // test int get_index_transaction(PublicKey publicKey)
  @Test
  public void getIndexTransactionTest() throws GeneralSecurityException {
    TransactionObjectBuilder builder = getValidTransactionBuilder();

    String type = "P2PKH";
    // RECEIVER
    String pubKeyHash = sender.computeHash();
    ScriptOutputObject scriptTxOut = new ScriptOutputObject(type, pubKeyHash);
    int value = 32;
    OutputObject output = new OutputObject(value, scriptTxOut);
    List<OutputObject> listOutput = Collections.singletonList(output);
    builder.setOutputs(listOutput);
    assertEquals(0, builder.build().getIndexTransaction(sender));
  }

  @Test
  public void computeIdTest() throws GeneralSecurityException {
    String path = "protocol/examples/messageData/coin/post_transaction_coinbase.json";
    String validJson = JsonTestUtils.loadFile(path);
    PostTransactionCoin postTransactionModel = (PostTransactionCoin) JsonTestUtils.parse(validJson);
    Transaction transactionModel = postTransactionModel.getTransaction();

    TransactionObjectBuilder builder = getValidTransactionBuilder();
    builder.setLockTime(transactionModel.getLockTime());
    builder.setVersion(transactionModel.getVersion());

    List<InputObject> inpObj = new ArrayList<>();
    List<OutputObject> outObj = new ArrayList<>();
    for (Input i : transactionModel.getInputs()) {
      ScriptInput scriptInput = i.getScript();
      inpObj.add(
          new InputObject(
              i.getTxOutHash(),
              i.getTxOutIndex(),
              new ScriptInputObject(
                  scriptInput.getType(), scriptInput.getPubkey(), scriptInput.getSig())));
    }

    for (Output o : transactionModel.getOutputs()) {
      ScriptOutput scriptOutput = o.getScript();
      outObj.add(
          new OutputObject(
              o.getValue(),
              new ScriptOutputObject(scriptOutput.getType(), scriptOutput.getPubKeyHash())));
    }

    builder.setInputs(inpObj);
    builder.setOutputs(outObj);

    TransactionObject transactionObject = builder.build();
    assertTrue(transactionObject.isCoinBaseTransaction());
  }

  private TransactionObjectBuilder getValidTransactionBuilder() throws GeneralSecurityException {
    return getValidTransactionBuilder("a");
  }

  private TransactionObjectBuilder getValidTransactionBuilder(String transactionId)
      throws GeneralSecurityException {
    TransactionObjectBuilder builder = new TransactionObjectBuilder();

    String type = "P2PKH";
    int txOutIndex = 0;
    String txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

    ScriptInputObject scriptTxIn;
    String sig = senderKey.sign(sender).getEncoded();
    scriptTxIn = new ScriptInputObject(type, sender, new Signature(sig));
    InputObject input = new InputObject(txOutHash, txOutIndex, scriptTxIn);
    List<InputObject> listInput = Collections.singletonList(input);
    builder.setInputs(listInput);

    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    builder.setChannel(channel);

    String pubKeyHash = sender.computeHash();
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
