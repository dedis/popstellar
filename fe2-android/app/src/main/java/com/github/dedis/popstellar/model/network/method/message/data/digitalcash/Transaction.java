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
import java.util.Map;
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

  public int get_version() {
    return version;
  }

  public List<Input> get_inputs() {
    return Collections.checkedList(inputs, Input.class);
  }

  public List<Output> get_outputs() {
    return Collections.checkedList(outputs, Output.class);
  }

  public long get_lock_time() {
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
      collect_transaction.add(currentTxin.get_script().get_pubkey());
      // Sig
      collect_transaction.add(currentTxin.get_script().get_sig());
      // Type
      collect_transaction.add(currentTxin.get_script().get_type());
      // TxOutHash
      collect_transaction.add(currentTxin.get_tx_out_hash());
      // TxOutIndex
      collect_transaction.add(String.valueOf(currentTxin.get_tx_out_index()));
    }

    // lock_time
    collect_transaction.add(String.valueOf(lock_time));
    // Outputs
    for (int i = 0; i < outputs.size(); i++) {
      Output currentTxout = outputs.get(i);
      // Script
      // PubKeyHash
      collect_transaction.add(currentTxout.getScript().get_pubkey_hash());
      // Type
      collect_transaction.add(currentTxout.getScript().get_type());
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

  /**
   * Function that given a key pair change the sig of an input considering all the outputs
   *
   * @param keyPair of one input sender
   * @return sig other all the outputs and inputs with the public key
   * @throws GeneralSecurityException
   */
  public static String computeSigOutputsPairTxOutHashAndIndex(
      KeyPair keyPair, List<Output> outputs, Map<String, Integer> inputs_pairs)
      throws GeneralSecurityException {
    // input #1: tx_out_hash Value //input #1: tx_out_index Value
    // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
    // TxOut #1: LaoCoin Value​​ //TxOut #1: script.type Value //TxOut #1: script.pubkey_hash Value
    // TxOut #2: LaoCoin Value​​ //TxOut #2: script.type Value //TxOut #2: script.pubkey_hash
    // Value...
    String[] sig = new String[inputs_pairs.size() * 2 + outputs.size() * 3];
    Iterator<Map.Entry<String, Integer>> ite_input = inputs_pairs.entrySet().iterator();
    Iterator<Output> ite_output = outputs.iterator();

    int index = 0;
    while (ite_input.hasNext()) {
      Map.Entry<String, Integer> current = ite_input.next();
      sig[index] = current.getKey();
      sig[index + 1] = String.valueOf(current.getValue());
      index = index + 2;
    }

    while (ite_output.hasNext()) {
      Output current = ite_output.next();
      sig[index] = String.valueOf(current.getValue());
      sig[index + 1] = current.getScript().get_type();
      sig[index + 2] = current.getScript().get_pubkey_hash();
      index = index + 3;
    }
    return keyPair.sign(new Base64URLData(String.join("", sig))).getEncoded();
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
    return Objects.hash(get_version(), get_inputs(), get_outputs(), get_lock_time());
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
