package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// Object representing an output of this transaction
public final class Output {

  @SerializedName("value")
  private final long value; // the value of the output transaction, expressed in miniLAOs

  @SerializedName("script")
  private final ScriptOutput script; // The script describing the TxOut unlock mechanism

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public Output(long value, ScriptOutput script) {
    this.value = value;
    this.script = script;
  }

  public long getValue() {
    return value;
  }

  public ScriptOutput getScript() {
    return script;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Output txOut = (Output) o;
    return value == txOut.value && script.equals(txOut.script);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue(), getScript());
  }

  @Override
  public String toString() {
    return "output{" + "value=" + value + ", script=" + script + '}';
  }
}
