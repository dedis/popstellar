package com.github.dedis.popstellar.model.objects;

public class InputObject {
  private final String txOutHash;
  private final int txOutIndex;
  private final ScriptInputObject script;

  /**
   * @param txOutHash Previous (to-be-used) transaction hash
   * @param txOutIndex index of the previous to-be-used transaction
   * @param script The script describing the unlock mechanism
   */
  public InputObject(String txOutHash, int txOutIndex, ScriptInputObject script) {
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

  public ScriptInputObject getScript() {
    return script;
  }
}
