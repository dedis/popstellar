package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject;

public class OutputObject {
  private final long value;
  private final ScriptOutputObject script;

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public OutputObject(long value, ScriptOutputObject script) {
    this.value = value;
    this.script = script;
  }

  public long getValue() {
    return value;
  }

  public ScriptOutputObject getScript() {
    return script;
  }

  public String getPubKeyHash() {
    return script.getPubKeyHash();
  }

  @Override
  public String toString() {
    return "OutputObject{" + "value=" + value + ", keyHash=" + getPubKeyHash() + '}';
  }
}
