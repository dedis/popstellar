package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class ConsensusFailure
/**
 * Constructor for a data Failure
 *
 * @param instanceId unique id of the consensus instance
 * @param messageId message id of the Elect message
 * @param creation UNIX timestamp in UTC
 */
(
  @field:SerializedName("instance_id") val instanceId: String,
  @field:SerializedName("message_id") val messageId: MessageID,
  @field:SerializedName("created_at") val creation: Long
) : Data() {

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.FAILURE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusFailure
    return creation == that.creation && instanceId == that.instanceId && messageId == that.messageId
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, creation)
  }

  override fun toString(): String {
    return "ConsensusFailure{instance_id='$instanceId', message_id='${messageId.encoded}', created_at=$creation}"
  }
}
