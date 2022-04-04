package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// Object representing an output of this transaction
public class TxOut {
  @SerializedName("Value")
  private final int value; // the value of the output transaction, expressed in miniLAOs

  @SerializedName("Script")
  private final ScriptTxOut script; // The script describing the TxOut unlock mechanism

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public TxOut(int value, ScriptTxOut script) {
    this.value = value;
    this.script = script;
  }

  public int getValue() {
    return value;
  }

  public ScriptTxOut getScript() {
    return script;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TxOut txOut = (TxOut) o;
    return value == txOut.value && script.equals(txOut.script);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue(), getScript());
  }

  @Override
  public String toString() {
    return "TxOut{" +
            "value=" + value + + '\'' +
            ", script=" + script + '\'' +
            '}';
  }
}
