package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Script_output;

public class Output_object {
  private final int value;
  private final Script_output script;

  /**
   * @param value the value of the output transaction, expressed in miniLAOs
   * @param script The script describing the TxOut unlock mechanism
   */
  public Output_object(int value, Script_output script) {
    this.value = value;
    this.script = script;
  }

  public int get_value() {
    return value;
  }

  public Script_output get_script() {
    return script;
  }
}
