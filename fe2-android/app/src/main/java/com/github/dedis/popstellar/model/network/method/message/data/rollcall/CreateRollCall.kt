package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.RollCall
import com.google.gson.annotations.SerializedName
import java.util.Optional

/** Data sent to create a Roll-Call  */
@Immutable
class CreateRollCall : Data {
    @JvmField
    val id: String

    @JvmField
    val name: String

    @JvmField
    val creation: Long

    @JvmField
    @SerializedName("proposed_start")
    val proposedStart: Long

    @JvmField
    @SerializedName("proposed_end")
    val proposedEnd: Long

    @JvmField
    val location: String
    private val description: String?

    /**
     * Constructor for a data Create Roll-Call Event
     *
     * @param name name of the Roll-Call
     * @param creation of the Roll-Call
     * @param proposedStart of the Roll-Call
     * @param proposedEnd of the Roll-Call
     * @param location location of the Roll-Call
     * @param description can be null
     * @param laoId ID of the LAO
     */
    constructor(
            name: String,
            creation: Long,
            proposedStart: Long,
            proposedEnd: Long,
            location: String,
            description: String?,
            laoId: String?) {
        this.name = name
        this.creation = creation
        this.proposedStart = proposedStart
        if (proposedEnd == 0L) {
            this.proposedEnd = proposedStart + 3600
        } else {
            this.proposedEnd = proposedEnd
        }
        this.location = location
        this.description = description
        id = RollCall.generateCreateRollCallId(laoId, creation, name)
    }

    constructor(
            id: String,
            name: String,
            creation: Long,
            proposedStart: Long,
            proposedEnd: Long,
            location: String,
            description: String?) {
        this.id = id
        this.name = name
        this.creation = creation
        this.proposedStart = proposedStart
        this.proposedEnd = proposedEnd
        this.location = location
        this.description = description
    }

    override val `object`: String
        get() = Objects.ROLL_CALL.`object`
    override val action: String
        get() = Action.CREATE.action

    fun getDescription(): Optional<String> {
        return Optional.ofNullable(description)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as CreateRollCall
        return creation == that.creation && proposedStart == that.proposedStart &&
                proposedEnd == that.proposedEnd && id == that.id && name == that.name &&
                location == that.location && getDescription() == that.getDescription()
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(
                id,
                name,
                creation,
                proposedStart,
                proposedEnd,
                location,
                getDescription())
    }

    override fun toString(): String {
        return ("CreateRollCall{"
                + "id='"
                + id
                + '\''
                + ", name='"
                + name
                + '\''
                + ", creation="
                + creation
                + ", proposedStart="
                + proposedStart
                + ", proposedEnd="
                + proposedEnd
                + ", location='"
                + location
                + '\''
                + ", description='"
                + description
                + '\''
                + '}')
    }
}