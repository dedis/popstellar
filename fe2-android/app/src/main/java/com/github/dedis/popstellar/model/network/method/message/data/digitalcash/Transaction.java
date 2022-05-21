package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

// the transaction object
public class Transaction {
  @SerializedName(value = "version")
  private final int version; // The version of the transaction inputs

  @SerializedName(value = "inputs")
  private List<Input> inputs; // [Array[Objects]] array of output transactions to use as inputs

  @SerializedName(value = "outputs")
  private List<Output> outputs; // [Array[Objects]] array of outputs from this transactions

  @SerializedName("lock_time")
  private final long lock_time; // LockTime

  /**
   * @param version The version of the transaction inputs
   * @param inputs [Array[Objects]] array of output transactions to use as inputs
   * @param outputs [Array[Objects]] array of outputs from this transactions
   * @param lock_time TimeStamp
   */
  public Transaction(int version, List<Input> inputs, List<Output> outputs, long lock_time) {
    this.version = version;
    this.inputs = Collections.checkedList(inputs, Input.class);
    this.outputs = Collections.checkedList(outputs, Output.class);
    this.lock_time = lock_time;
    // change the sig for all the inputs

  }

  public int getVersion() {
    return version;
  }

  public List<Input> getInputs() {
    return Collections.checkedList(inputs, Input.class);
  }

  public List<Output> getOutputs() {
    return Collections.checkedList(outputs, Output.class);
  }

  public long getLockTime() {
    return lock_time;
  }

  public String computeId() {
    // Make a list all the string in the transaction
    List<String> collect_transaction = new ArrayList<String>();
    // Add them in lexicographic order

    // Inputs
    for (int i = 0; i < inputs.size(); i++) {
      Input currentTxin = inputs.get(i);
      // Script
      // PubKey
      collect_transaction.add(currentTxin.getScript().getPubkey());
      // Sig
      collect_transaction.add(currentTxin.getScript().getSig());
      // Type
      collect_transaction.add(currentTxin.getScript().get_type());
      // TxOutHash
      collect_transaction.add(currentTxin.getTxOutHash());
      // TxOutIndex
      collect_transaction.add(String.valueOf(currentTxin.getTxOutIndex()));
    }

    // lock_time
    collect_transaction.add(String.valueOf(lock_time));
    // Outputs
    for (int i = 0; i < outputs.size(); i++) {
      Output currentTxout = outputs.get(i);
      // Script
      // PubKeyHash
      collect_transaction.add(currentTxout.getScript().getPubkeyHash());
      // Type
      collect_transaction.add(currentTxout.getScript().getType());
      // Value
      collect_transaction.add(String.valueOf(currentTxout.getValue()));
    }
    // Version
    collect_transaction.add(String.valueOf(version));

    String concat = "";
    for (int i = 0; i < collect_transaction.size(); i++) {
      String to_add = collect_transaction.get(i);
      concat = concat.concat(String.valueOf(to_add.length()) + to_add);
    }

    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(concat.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      System.err.println("Something is wrong");
      throw new IllegalArgumentException("Error in the computation of the transaction id");
    }
  }

  public void changeSigInputsConsideringTheOutputs(KeyPair keyPair)
      throws GeneralSecurityException {
    String sig = computeSigOutputsInputs(keyPair, inputs, outputs);
    Iterator<Input> ite = inputs.iterator();
    while (ite.hasNext()) {
      Input current = ite.next();
      if (keyPair.getPublicKey().computeHash().equals(current.getScript().getPubkey())) {
        current.getScript().setSig(sig);
      }
    }
  }

  public static String computeSigOutputsInputs(
      KeyPair keyPair, List<Input> inputs, List<Output> outputs) throws GeneralSecurityException {
    // input #1: tx_out_hash Value //input #1: tx_out_index Value
    // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
    // TxOut #1: LaoCoin Value​​ //TxOut #1: script.type Value //TxOut #1: script.pubkey_hash Value
    // TxOut #2: LaoCoin Value​​ //TxOut #2: script.type Value //TxOut #2: script.pubkey_hash
    // Value...
    String[] sig = new String[inputs.size() * 2 + outputs.size() * 3];
    Iterator<Input> ite_input = inputs.iterator();
    Iterator<Output> ite_output = outputs.iterator();
    int index = 0;
    while (ite_input.hasNext()) {
      Input current = ite_input.next();
      sig[index] = current.getTxOutHash();
      sig[index + 1] = String.valueOf(current.getTxOutIndex());
      index = index + 2;
    }

    while (ite_output.hasNext()) {
      Output current = ite_output.next();
      sig[index] = String.valueOf(current.getValue());
      sig[index + 1] = current.getScript().getType();
      sig[index + 2] = current.getScript().getPubkeyHash();
      index = index + 3;
    }
    Base64URLData signature = new Base64URLData(String.join("", sig));
    return keyPair.sign(signature).getEncoded();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transaction that = (Transaction) o;
    return version == that.version
        && lock_time == that.lock_time
        && inputs.equals(that.inputs)
        && outputs.equals(that.outputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getVersion(), getInputs(), getOutputs(), getLockTime());
  }

  @Override
  public String toString() {
    return "Transaction{"
        + "version="
        + version
        + ", inputs="
        + Arrays.toString(inputs.toArray())
        + ", outputs="
        + Arrays.toString(outputs.toArray())
        + ", lock_time="
        + lock_time
        + '}';
  }
}
