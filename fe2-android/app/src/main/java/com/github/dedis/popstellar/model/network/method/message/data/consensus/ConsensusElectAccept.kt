package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class ConsensusElectAccept
/**
 * Constructor for a data Elect_Accept
 *
 * @param instanceId unique id of the consensus instance
 * @param messageId message id of the Elect message
 * @param isAccept true if the node agrees with the proposal
 */
(
  @field:SerializedName("instance_id") val instanceId: String,
  @field:SerializedName("message_id") val messageId: MessageID,
  @field:SerializedName("accept") val isAccept: Boolean
) : Data() {

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.ELECT_ACCEPT.action

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, isAccept)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusElectAccept
    return isAccept == that.isAccept && instanceId == that.instanceId && messageId == that.messageId
  }

  override fun toString(): String {
    return "ConsensusElectAccept{instance_id='$instanceId', message_id='${messageId.encoded}', accept=$isAccept}"
  }
}
