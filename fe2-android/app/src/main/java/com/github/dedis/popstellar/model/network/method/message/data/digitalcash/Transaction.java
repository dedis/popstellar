package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// the transaction object
public class Transaction {
  @SerializedName(value = "Version")
  private final int version; // The version of the transaction inputs

  @SerializedName(value = "TxIn")
  private final List<TxIn> TxIns; // [Array[Objects]] array of output transactions to use as inputs

  @SerializedName(value = "TxOut")
  private final List<TxOut> TxOuts; // [Array[Objects]] array of outputs from this transactions

  @SerializedName("LockTime")
  private final long timestamp; // TimeStamp

  /**
   * @param version The version of the transaction inputs
   * @param TxIns [Array[Objects]] array of output transactions to use as inputs
   * @param TxOuts [Array[Objects]] array of outputs from this transactions
   * @param timestamp TimeStamp
   */
  public Transaction(int version, List<TxIn> TxIns, List<TxOut> TxOuts, long timestamp) {
    this.version = version;
    this.TxIns = Collections.checkedList(TxIns, TxIn.class);
    this.TxOuts = Collections.checkedList(TxOuts, TxOut.class);
    this.timestamp = timestamp;
  }

  public int getVersion() {
    return version;
  }

  public List<TxIn> getTxIns() {
    return Collections.checkedList(TxIns, TxIn.class);
  }

  public List<TxOut> getTxOuts() {
    return Collections.checkedList(TxOuts, TxOut.class);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String computeId() {
    // Make a list all the string in the transaction
    List<String> collect_transaction = new ArrayList<String>();
    // Add them in lexicographic order

    // Timestamp
    collect_transaction.add(String.valueOf(timestamp));
    // TxIns
    for (int i = 0; i < TxIns.size(); i++) {
      TxIn currentTxin = TxIns.get(i);
      // Script
      // PubKey
      collect_transaction.add(currentTxin.getScript().getPub_key_recipient());
      // Sig
      collect_transaction.add(currentTxin.getScript().getSig());
      // Type
      collect_transaction.add(currentTxin.getScript().getType());
      // TxOutHash
      collect_transaction.add(currentTxin.getTxOutHash());
      // TxOutIndex
      collect_transaction.add(String.valueOf(currentTxin.getTxOutIndex()));
    }
    // TxOuts
    for (int i = 0; i < TxOuts.size(); i++) {
      TxOut currentTxout = TxOuts.get(i);
      // Script
      // PubKeyHash
      collect_transaction.add(currentTxout.getScript().getPub_key_hash());
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
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Something is wrong");
    }

    byte[] hash = digest.digest(concat.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().encodeToString(hash);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Transaction that = (Transaction) o;
    return version == that.version
        && timestamp == that.timestamp
        && TxIns.equals(that.TxIns)
        && TxOuts.equals(that.TxOuts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getVersion(), getTxIns(), getTxOuts(), getTimestamp());
  }

  @Override
  public String toString() {
    return "Transaction{"
        + "version="
        + version
        + ", TxIns="
        + Arrays.toString(TxIns.toArray())
        + ", TxOuts="
        + Arrays.toString(TxOuts.toArray())
        + ", timestamp="
        + timestamp
        + '}';
  }
}
