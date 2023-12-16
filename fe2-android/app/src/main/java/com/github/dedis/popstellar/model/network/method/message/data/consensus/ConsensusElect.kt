package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.google.gson.annotations.SerializedName

class ConsensusElect(@JvmField @field:SerializedName("created_at") val creation: Long,
                     objId: String?,
                     type: String?,
                     property: String?,
                     value: Any) : Data() {
    @JvmField
    @SerializedName("instance_id")
    val instanceId: String

    @JvmField
    val key: ConsensusKey

    @JvmField
    val value: Any

    init {
        instanceId = ElectInstance.generateConsensusId(type!!, objId!!, property!!)
        key = ConsensusKey(type, objId, property)
        this.value = value
    }

    override val `object`: String
        get() = Objects.CONSENSUS.`object`
    override val action: String
        get() = Action.ELECT.action

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
        return creation == that.creation && instanceId == that.instanceId && key == that.key && value == that.value
    }

    override fun toString(): String {
        return String.format(
                "ConsensusElect{instance_id='%s', created_at=%s, key='%s', value='%s'}",
                instanceId, creation, key, value)
    }
}