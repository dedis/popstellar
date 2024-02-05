package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.model.objects.security.PublicKey

@Immutable
class OutputObject
/**
 * @param value the value of the output transaction, expressed in miniLAOs
 * @param script The script describing the TxOut unlock mechanism
 */
(val value: Long, val script: ScriptOutputObject) {

  val pubKeyHash: String
    get() = script.pubKeyHash

  fun isUserOutputRecipient(user: PublicKey): Boolean {
    return script.pubKeyHash == user.computeHash()
  }

  override fun toString(): String {
    return "OutputObject{value=$value, keyHash=$pubKeyHash}"
  }
}
