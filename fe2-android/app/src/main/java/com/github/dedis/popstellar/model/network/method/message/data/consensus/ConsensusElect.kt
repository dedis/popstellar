package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.google.gson.annotations.SerializedName

class ConsensusElect(
    @field:SerializedName("created_at") val creation: Long,
    objId: String,
    type: String,
    property: String,
    val value: Any
) : Data {
  @SerializedName("instance_id")
  val instanceId: String = ElectInstance.generateConsensusId(type, objId, property)

  val key: ConsensusKey = ConsensusKey(type, objId, property)

  override val `object`: String = Objects.CONSENSUS.`object`
  override val action: String = Action.ELECT.action

  override fun hashCode(): Int {
    return java.util.Objects.hash(instanceId, creation, key, value)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ConsensusElect
    return creation == that.creation &&
        instanceId == that.instanceId &&
        key == that.key &&
        value == that.value
  }

  override fun toString(): String {
    return "ConsensusElect{instance_id='$instanceId', created_at=$creation, key='$key', value='$value'}"
  }
}
