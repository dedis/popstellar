package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.security.PublicKey

@Immutable
class InputObject
/**
 * @param txOutHash Previous (to-be-used) transaction hash
 * @param txOutIndex index of the previous to-be-used transaction
 * @param script The script describing the unlock mechanism
 */
(val txOutHash: String, val txOutIndex: Int?, val script: ScriptInputObject) {

  val pubKey: PublicKey
    get() = script.pubKey

  override fun toString(): String {
    return "InputObject{txOutHash='$txOutHash', txOutIndex=$txOutIndex, key=$pubKey}"
  }
}
