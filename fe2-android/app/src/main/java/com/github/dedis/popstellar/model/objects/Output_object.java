package com.github.dedis.popstellar.model.objects;

public class Output_object {
  private final int value;
  private final Script_output_object script;

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public Output_object(int value, Script_output_object script) {
    this.value = value;
    this.script = script;
  }

  public int get_value() {
    return value;
  }

  public Script_output_object get_script() {
    return script;
  }
}
