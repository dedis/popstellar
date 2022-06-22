package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.utility.security.Hash;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The transaction object
 */
public final class Transaction {
  @SerializedName(value = "version")
  private final int version; // The version of the transaction inputs

  @SerializedName(value = "inputs")
  private List<Input> inputs; // [Array[Objects]] array of output transactions to use as inputs

  @SerializedName(value = "outputs")
  private List<Output> outputs; // [Array[Objects]] array of outputs from this transactions

  @SerializedName("lock_time")
  private final long lockTime; // LockTime

  /**
   * Transaction constructor
   * @param version The version of the transaction inputs
   * @param inputs [Array[Objects]] array of output transactions to use as inputs
   * @param outputs [Array[Objects]] array of outputs from this transactions
   * @param lockTime TimeStamp
   */
  public Transaction(int version, List<Input> inputs, List<Output> outputs, long lockTime) {
    this.version = version;
    this.inputs = Collections.unmodifiableList(inputs);
    this.outputs = Collections.unmodifiableList(outputs);
    this.lockTime = lockTime;
    // change the sig for all the inputs

  }

  public int getVersion() {
    return version;
  }

  public List<Input> getInputs() {
    return inputs;
  }

  public List<Output> getOutputs() {
    return outputs;
  }

  public long getLockTime() {
    return lockTime;
  }

  public String computeId() {
    // Make a list all the string in the transaction
    List<String> collectTransaction = new ArrayList<>();
    // Add them in lexicographic order

    // Inputs
    for (Input currentTxin : inputs) {
      // Script
      // PubKey
      collectTransaction.add(currentTxin.getScript().getPubkey().getEncoded());
      // Sig
      collectTransaction.add(currentTxin.getScript().getSig().getEncoded());
      // Type
      collectTransaction.add(currentTxin.getScript().getType());
      // TxOutHash
      collectTransaction.add(currentTxin.getTxOutHash());
      // TxOutIndex
      collectTransaction.add(String.valueOf(currentTxin.getTxOutIndex()));
    }

    // lock_time
    collectTransaction.add(String.valueOf(lockTime));
    // Outputs
    for (Output currentTxout : outputs) {
      // Script
      // PubKeyHash
      collectTransaction.add(currentTxout.getScript().getPubkeyHash());
      // Type
      collectTransaction.add(currentTxout.getScript().getType());
      // Value
      collectTransaction.add(String.valueOf(currentTxout.getValue()));
    }
    // Version
    collectTransaction.add(String.valueOf(version));

    // Use already implemented hash function
    return Hash.hash(collectTransaction.toArray(new String[0]));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transaction that = (Transaction) o;
    return version == that.version
        && lockTime == that.lockTime
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
        + lockTime
        + '}';
  }

  /**
   * Function that given a key pair change the sig of an input considering all the outputs
   *
   * @return sig other all the outputs and inputs with the public key
   */
  public static String computeSigOutputsPairTxOutHashAndIndex(
      List<Output> outputs, Map<String, Integer> inputsPairs) {
    // input #1: tx_out_hash Value //input #1: tx_out_index Value
    // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
    // TxOut #1: LaoCoin Value​​ //TxOut #1: script.type Value //TxOut #1: script.pubkey_hash Value
    // TxOut #2: LaoCoin Value​​ //TxOut #2: script.type Value //TxOut #2: script.pubkey_hash
    // Value...
    List<String> sig = new ArrayList<>();

    for (Map.Entry<String, Integer> current: inputsPairs.entrySet()) {
      sig.add(current.getKey());
      sig.add(String.valueOf(current.getValue()));
    }

    for (Output current: outputs) {
      sig.add(String.valueOf(current.getValue()));
      sig.add(current.getScript().getType());
      sig.add(current.getScript().getPubkeyHash());
    }

    return String.join("", sig.toArray(new String[0]));
  }

}
