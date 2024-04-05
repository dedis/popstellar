package com.github.dedis.popstellar.model.network.method.message.data.gossiping

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName

/** Data sent when propagating a rumor */
@Immutable
class Rumor
/**
 * Constructor for a Data Rumor
 *
 * @param senderId the publish key of the sender's server
 * @param rumorId ID of the rumor
 * @param messages the messages being propagated as part of this rumor
 */(
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("rumor_id") val rumorId: Int,
    @SerializedName("messages") val messages: List<Map<String, List<Any>>>
) : Data {

    init {
        verify().isNotEmptyBase64(senderId, "Sender ID")
    }

    override val `object`: String
        get() = Objects.GOSSIP.`object`

    override val action: String
        get() = Action.RUMOR.action

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val rumor = other as Rumor

        return  senderId != rumor.senderId && rumorId != rumor.rumorId && messages == rumor.messages
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(senderId, rumorId, messages)
    }

    override fun toString(): String {
        return "Rumor(senderId='$senderId', rumorId=$rumorId, messages=$messages)"
    }
}