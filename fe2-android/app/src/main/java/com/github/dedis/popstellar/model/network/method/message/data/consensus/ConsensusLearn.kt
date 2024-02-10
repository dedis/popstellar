package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName
import java.util.Collections

@Immutable
/**
 * Constructor for a data Learn
 *
 * @param instanceId unique id of the consensus instance
 * @param messageId message id of the Elect message
 * @param creation UNIX timestamp in UTC
 * @param decision true if the consensus was successful
 * @param acceptorSignatures signatures of all the received Accept messages
 */
class ConsensusLearn(
    @field:SerializedName("instance_id") val instanceId: String,
    @field:SerializedName("message_id") val messageId: MessageID,
    @field:SerializedName("created_at") val creation: Long,
    decision: Boolean,
    acceptorSignatures: List<String>
) : Data {

  @SerializedName("value") val learnValue: LearnValue = LearnValue(decision)

  @SerializedName("acceptor-signatures")
  val acceptorSignatures: List<String> = Collections.unmodifiableList(acceptorSignatures)

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.LEARN.action

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, creation, learnValue, acceptorSignatures)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusLearn
    return creation == that.creation &&
        instanceId == that.instanceId &&
        messageId == that.messageId &&
        learnValue == that.learnValue &&
        acceptorSignatures == that.acceptorSignatures
  }

  override fun toString(): String {
    return "ConsensusLearn{instance_id='$instanceId', message_id='${messageId.encoded}', " +
        "acceptor-signatures=${acceptorSignatures.toTypedArray().contentToString()}"
  }
}
