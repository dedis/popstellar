package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// "Object representing a transaction output to use as an input for this transaction"
public class Input {
  @SerializedName("tx_out_hash")
  private final String txOutHash; // Previous (to-be-used) transaction hash

  @SerializedName("tx_out_index")
  private final int txOutIndex; // index of the previous to-be-used transaction

  @SerializedName("script")
  private final Script_input script; // The script describing the unlock mechanism

  /**
   * @param txOutHash Previous (to-be-used) transaction hash
   * @param txOutIndex index of the previous to-be-used transaction
   * @param script The script describing the unlock mechanism
   */
  public Input(String txOutHash, int txOutIndex, Script_input script) {
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

  @Override
  public String toString() {
    return "input{"
        + "tx_out_hash='"
        + txOutHash
        + '\''
        + ", tx_out_index="
        + txOutIndex
        + ", script="
        + script
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Input txIn = (Input) o;
    return txOutIndex == txIn.txOutIndex
        && txOutHash.equals(txIn.txOutHash)
        && script.equals(txIn.script);
  }

  @Override
  public int hashCode() {
    return Objects.hash(get_tx_out_hash(), get_tx_out_index(), get_script());
  }
}
