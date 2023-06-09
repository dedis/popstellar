package be.model;

import be.utils.Hash;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;
import common.utils.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * A simplified version of the transaction system in the android frontend.
 * So far this only works for the initial transaction and does not keep track of previous transactions!
 * TODO: extend this to work for several transactions
 * Used to compute some valid values for simple transactions.
 */
public class Transaction {
  public int version = 1;
  public List<Input> inputs;
  public List<Output> outputs;
  public long lockTime;
  public String tx_out_hash_coinbase = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  public String type = "P2PKH";

  public Transaction() {
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
    this.lockTime = 0;
  }

  /**
   * Simplified version of transaction computations in DigitalCashViewModel in android frontend.
   * This only works for one transaction and does not keep track of previous transactions!
   *
   * @return a transaction with updated input and output values
   * @throws GeneralSecurityException
   */
  public void issueInitialCoins(String receiverPublicKey, String senderPublicKey, String senderPrivateKey, long amountToGive)
    throws GeneralSecurityException {
    Output output = new Output(
      amountToGive,
      new ScriptOutput(type, Hash.hash(receiverPublicKey.getBytes(StandardCharsets.UTF_8))));
    outputs.add(output);

    Map<String, Integer> inputsPairs = new HashMap<>();
    inputsPairs.put(tx_out_hash_coinbase, 0);

    byte[] toSign = Transaction.computeSigOutputsPairTxOutHashAndIndex(outputs, inputsPairs)
      .getBytes(StandardCharsets.UTF_8);

    // Create a signature for the transaction using the sender's private key
    PublicKeySign publicKeySign = new Ed25519Sign(Base64Utils.decode(senderPrivateKey));
    byte[] signBytes = publicKeySign.sign(toSign);
    String signature = Base64Utils.encode(signBytes);

    // Create an input for the transaction using the coinbase transaction hash
    Input input = new Input(tx_out_hash_coinbase, 0, new ScriptInput(type, senderPublicKey, signature));

    inputs.add(input);
  }

  /**
   * Copied from Transaction data class in android frontend
   *
   * @return signature of all the outputs and inputs with the public key
   */
  public static String computeSigOutputsPairTxOutHashAndIndex(
    List<Output> outputs, Map<String, Integer> inputsPairs) {
    // input #1: tx_out_hash Value //input #1: tx_out_index Value
    // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
    // TxOut #1: LaoCoin Value​​ //TxOut #1: script.type Value //TxOut #1: script.pubkey_hash Value
    // TxOut #2: LaoCoin Value​​ //TxOut #2: script.type Value //TxOut #2: script.pubkey_hash
    // Value...
    List<String> sig = new ArrayList<>();

    for (Map.Entry<String, Integer> current : inputsPairs.entrySet()) {
      sig.add(current.getKey());
      sig.add(String.valueOf(current.getValue()));
    }

    for (Output current : outputs) {
      sig.add(String.valueOf(current.value));
      sig.add(current.script.type);
      sig.add(current.script.pubKeyHash);
    }

    return String.join("", sig.toArray(new String[0]));
  }

  /**
   * Copied from Transaction data class in android frontend
   *
   * @return the transaction id computed based on the previous inputs and outputs.
   */
  public String computeId() {
    // Make a list all the string in the transaction
    List<String> collectTransaction = new ArrayList<>();
    // Add them in lexicographic order

    // Inputs
    for (Input currentTxin : inputs) {
      // Script
      // PubKey
      collectTransaction.add(currentTxin.script.pubKeyRecipient);
      // Sig
      collectTransaction.add(currentTxin.script.sig);
      // Type
      collectTransaction.add(currentTxin.script.type);
      // TxOutHash
      collectTransaction.add(currentTxin.txOutHash);
      // TxOutIndex
      collectTransaction.add(String.valueOf(currentTxin.txOutIndex));
    }

    // lock_time
    collectTransaction.add(String.valueOf(lockTime));
    // Outputs
    for (Output currentTxout : outputs) {
      // Script
      // PubKeyHash
      collectTransaction.add(currentTxout.script.pubKeyHash);
      // Type
      collectTransaction.add(currentTxout.script.type);
      // Value
      collectTransaction.add(String.valueOf(currentTxout.value));
    }
    // Version
    collectTransaction.add(String.valueOf(version));

    // Use already implemented hash function
    return Hash.hash(collectTransaction.toArray(new String[0]));
  }

  /**
   * @return an object containing the data to create a valid transaction post message
   */
  public PostTransaction post(){
    return new PostTransaction();
  }

  /** Contains the data to create a valid post transaction message */
  public class PostTransaction{
    public String transactionId;
    public PostTransaction(){
      this.transactionId = computeId();
    }
  }

  /** The following classes are all simplified versions of the digital cash data types in the android frontend */

  public class Input{
    public String txOutHash; // Previous (to-be-used) transaction hash
    public int txOutIndex; // index of the previous to-be-used transaction
    public ScriptInput script; // The script describing the unlock mechanism

    public Input(String txOutHash, int txOutIndex, ScriptInput script) {
      this.txOutHash = txOutHash;
      this.txOutIndex = txOutIndex;
      this.script = script;
    }

    @Override
    public String toString(){
      return "txOutHash: " + txOutHash
        + "  txOutIndex: " + txOutIndex
        + "  pubKeyRecipient: " + script.pubKeyRecipient
        + "  type: " + script.type
        + "  sig: " + script.sig;
    }
  }

  public class Output{
    public long value; // the value of the output transaction, expressed in miniLAOs
    public ScriptOutput script; // The script describing the TxOut unlock mechanism

    public Output(long value, ScriptOutput script) {
      this.value = value;
      this.script = script;
    }

    @Override
    public String toString(){
      return "value: " + value + "  pubKeyHash: " + script.pubKeyHash + "  type: " + script.type;
    }
  }

  public class ScriptOutput{
    public String type; // Type of script
    public String pubKeyHash;  // Hash of the recipient’s public key

    public ScriptOutput(String type, String pubKeyHash) {
      this.type = type;
      this.pubKeyHash = pubKeyHash;
    }
  }

  public class ScriptInput{
    public String type; // The script describing the unlock mechanism
    public String pubKeyRecipient; // The recipient’s public key
    public String sig; // Signature on all txins and txouts using the recipient's private key

    public ScriptInput(String type, String pubKeyRecipient, String sig) {
      this.type = type;
      this.pubKeyRecipient = pubKeyRecipient;
      this.sig = sig;
    }
  }
}
