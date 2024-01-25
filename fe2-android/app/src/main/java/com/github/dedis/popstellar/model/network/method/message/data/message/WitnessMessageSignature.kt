package com.github.dedis.popstellar.model.network.method.message.data.message

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.gson.annotations.SerializedName

/** Data sent to attest the message as a witness */
@Immutable
class WitnessMessageSignature
/**
 * Constructor for a data Witness Message Signature
 *
 * @param messageId ID of the message
 * @param signature signature by the witness over the message ID
 */
(@field:SerializedName("message_id") val messageId: MessageID, val signature: Signature) : Data {

  override val `object`: String
    get() = Objects.MESSAGE.`object`

  override val action: String
    get() = Action.WITNESS.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as WitnessMessageSignature
    return messageId == that.messageId && signature == that.signature
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(messageId, signature)
  }

  override fun toString(): String {
    return "WitnessMessageSignature{messageId='$messageId', signature='$signature'}"
  }
}
