package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// "Object representing a transaction output to use as an input for this transaction"
public class TxIn {
  @SerializedName("TxOutHash")
  private final String txOutHash; // Previous (to-be-used) transaction hash

  @SerializedName("TxOutIndex")
  private final int txOutIndex; // index of the previous to-be-used transaction

  @SerializedName("Script")
  private final ScriptTxIn script; // The script describing the unlock mechanism

  /**
   * @param txOutHash Previous (to-be-used) transaction hash
   * @param txOutIndex index of the previous to-be-used transaction
   * @param script The script describing the unlock mechanism
   */
  public TxIn(String txOutHash, int txOutIndex, ScriptTxIn script) {
    this.script = script;
    this.txOutHash = txOutHash;
    this.txOutIndex = txOutIndex;
  }

  public String getTxOutHash() {
    return txOutHash;
  }

  public int getTxOutIndex() {
    return txOutIndex;
  }

  public ScriptTxIn getScript() {
    return script;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TxIn txIn = (TxIn) o;
    return txOutIndex == txIn.txOutIndex
        && txOutHash.equals(txIn.txOutHash)
        && script.equals(txIn.script);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTxOutHash(), getTxOutIndex(), getScript());
  }

  @Override
  public String toString() {
    return "TxIn{"
        + "txOutHash='"
        + txOutHash
        + '\''
        + ", txOutIndex="
        + txOutIndex
        + '\''
        + ", script="
        + script
        + '\''
        + '}';
  }
}
