package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class ConsensusAccept(
    @field:SerializedName("instance_id") val instanceId: String,
    @field:SerializedName("message_id") val messageId: MessageID,
    @field:SerializedName("created_at") val creation: Long,
    acceptedTry: Int,
    acceptedValue: Boolean
) : Data() {

  @SerializedName("value") val acceptValue: AcceptValue

  /**
   * Constructor for a data Accept
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param acceptedTry the accepted try number
   * @param acceptedValue the value accepted
   */
  init {
    acceptValue = AcceptValue(acceptedTry, acceptedValue)
  }

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.ACCEPT.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusAccept
    return creation == that.creation &&
        instanceId == that.instanceId &&
        messageId == that.messageId &&
        acceptValue == that.acceptValue
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, creation, acceptValue)
  }

  override fun toString(): String {
    return "ConsensusAccept{instance_id='$instanceId', message_id='${messageId.encoded}', created_at=$creation, value=$acceptValue}"
  }
}
