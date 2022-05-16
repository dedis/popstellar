package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Script_input;

public class Input_object {
  private final String txOutHash;
  private final int txOutIndex;
  private final Script_input script;

  /**
   * @param txOutHash Previous (to-be-used) transaction hash
   * @param txOutIndex index of the previous to-be-used transaction
   * @param script The script describing the unlock mechanism
   */
  public Input_object(String txOutHash, int txOutIndex, Script_input script) {
    this.script = script;
    this.txOutHash = txOutHash;
    this.txOutIndex = txOutIndex;
  }

  public String get_tx_out_hash() {
    return txOutHash;
  }

  public int get_tx_out_index() {
    return txOutIndex;
  }

  public Script_input get_script() {
    return script;
  }
}
