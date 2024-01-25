package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.google.gson.annotations.SerializedName

@Immutable
class ConsensusPromise(
    @field:SerializedName("instance_id") val instanceId: String,
    @field:SerializedName("message_id") val messageId: MessageID,
    @field:SerializedName("created_at") val creation: Long,
    acceptedTry: Int,
    acceptedValue: Boolean,
    promisedTry: Int
) : Data {

  @SerializedName("value") val promiseValue: PromiseValue

  /**
   * Constructor for a data Promise
   *
   * @param instanceId unique id of the consensus instance
   * @param messageId message id of the Elect message
   * @param creation UNIX timestamp in UTC
   * @param acceptedTry previous accepted try number
   * @param acceptedValue previous accepted value
   * @param promisedTry promised try number
   */
  init {
    promiseValue = PromiseValue(acceptedTry, acceptedValue, promisedTry)
  }

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.PROMISE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusPromise
    return creation == that.creation &&
        instanceId == that.instanceId &&
        messageId == that.messageId &&
        promiseValue == that.promiseValue
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, messageId, creation, promiseValue)
  }

  override fun toString(): String {
    return "ConsensusPromise{instance_id='$instanceId', message_id='${messageId.encoded}', " +
        "created_at=$creation, value=$promiseValue}"
  }
}
