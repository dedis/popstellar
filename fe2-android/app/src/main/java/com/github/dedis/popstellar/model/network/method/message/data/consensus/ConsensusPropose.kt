package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class ConsensusPropose(
    @field:SerializedName("instance_id") val instanceId: String,
    @field:SerializedName("message_id") val messageId: MessageID,
    @field:SerializedName("created_at") val creation: Long,
    proposedTry: Int,
    proposedValue: Boolean,
    acceptorSignatures: List<String>
) : Data {

  @SerializedName("value") val proposeValue: ProposeValue

  @SerializedName("acceptor-signatures") private val acceptorSignatures: List<String>

  /**
   * Constructor for a data Propose
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param proposedTry proposed try number used in Paxos
   * @param proposedValue proposed value
   * @param acceptorSignatures signatures of all received Promise messages
   */
  init {
    proposeValue = ProposeValue(proposedTry, proposedValue)
    this.acceptorSignatures = ArrayList(acceptorSignatures)
  }

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.PROPOSE.action

  fun getAcceptorSignatures(): List<String> {
    return ArrayList(acceptorSignatures)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusPropose
    return creation == that.creation &&
        instanceId == that.instanceId &&
        messageId == that.messageId &&
        proposeValue == that.proposeValue &&
        acceptorSignatures == that.acceptorSignatures
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, creation, proposeValue, acceptorSignatures)
  }

  override fun toString(): String {
    return "ConsensusPropose{instance_id='$instanceId', message_id='${messageId.encoded}', created_at=$creation, value=$proposeValue, acceptor-signatures=${
      acceptorSignatures.toTypedArray().contentToString()
    }}"
  }
}
