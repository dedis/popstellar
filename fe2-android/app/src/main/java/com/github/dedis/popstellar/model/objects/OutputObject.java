package com.github.dedis.popstellar.model.objects;

public class OutputObject {
  private final int value;
  private final ScriptOutputObject script;

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public OutputObject(int value, ScriptOutputObject script) {
    this.value = value;
    this.script = script;
  }

  public int getValue() {
    return value;
  }

  public ScriptOutputObject getScript() {
    return script;
  }
}
