package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class ConsensusPrepare(
    @field:SerializedName("instance_id") val instanceId: String,
    @field:SerializedName("message_id") val messageId: MessageID,
    @field:SerializedName("created_at") val creation: Long,
    proposedTry: Int
) : Data {

  @SerializedName("value") val prepareValue: PrepareValue

  /**
   * Constructor for a data Prepare
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param proposedTry proposed try number
   */
  init {
    prepareValue = PrepareValue(proposedTry)
  }

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.PREPARE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusPrepare
    return creation == that.creation &&
        instanceId == that.instanceId &&
        messageId == that.messageId &&
        prepareValue == that.prepareValue
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, creation, prepareValue)
  }

  override fun toString(): String {
    return "ConsensusPrepare{instance_id='$instanceId', message_id='${messageId.encoded}', " +
        "created_at=$creation, value=$prepareValue}"
  }
}
