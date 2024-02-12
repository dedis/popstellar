package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Objects

/** Object representing a transaction output to use as an input for this transaction */
@Immutable
class Input
/**
 * @param txOutHash Previous (to-be-used) transaction hash
 * @param txOutIndex index of the previous to-be-used transaction
 * @param script The script describing the unlock mechanism
 */
(
    // Previous (to-be-used) transaction hash
    @field:SerializedName("tx_out_hash")
    val txOutHash: String, // index of the previous to-be-used transaction
    @field:SerializedName("tx_out_index")
    val txOutIndex: Int, // The script describing the unlock mechanism
    @field:SerializedName("script") val script: ScriptInput
) {

  override fun toString(): String {
    return "input{tx_out_hash='$txOutHash', tx_out_index=$txOutIndex, script=$script}"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val txIn = other as Input
    return txOutIndex == txIn.txOutIndex && txOutHash == txIn.txOutHash && script == txIn.script
  }

  override fun hashCode(): Int {
    return Objects.hash(txOutHash, txOutIndex, script)
  }
}
