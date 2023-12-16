package com.github.dedis.popstellar.model.network.method.message.data.lao

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName

/** Data received to track the state of a lao  */
class StateLao @Immutable constructor(
        id: String,
        name: String,
        creation: Long,
        lastModified: Long,
        organizer: PublicKey,
        modificationId: MessageID,
        witnesses: Set<PublicKey>,
        modificationSignatures: List<PublicKeySignaturePair>?) : Data() {
    @JvmField
    val id: String

    @JvmField
    val name: String

    @JvmField
    val creation: Long

    @JvmField
    @SerializedName("last_modified")
    val lastModified: Long

    @JvmField
    val organizer: PublicKey

    @JvmField
    @SerializedName("modification_id")
    val modificationId: MessageID
    private val witnesses: Set<PublicKey>

    @SerializedName("modification_signatures")
    private val modificationSignatures: MutableList<PublicKeySignaturePair>

    /**
     * Constructor for a Data StateLao
     *
     * @param id of the LAO state message, Hash(organizer||creation||name)
     * @param name name of the LAO
     * @param creation time of creation
     * @param lastModified time of last modification
     * @param organizer id of the LAO's organizer
     * @param modificationId id of the modification (either creation/update)
     * @param witnesses list of witnesses of the LAO
     * @param modificationSignatures signatures of the witnesses on the modification message (either
     * creation/update)
     * @throws IllegalArgumentException if arguments are invalid
     */
    init {
        // Organizer and witnesses are checked to be base64 at deserialization
        MessageValidator.verify()
                .orderedTimes(creation, lastModified)
                .validPastTimes(creation, lastModified)
                .validLaoId(id, organizer, creation, name)
                .isNotEmptyBase64(modificationId.encoded, "Modification ID")
        this.id = id
        this.name = name
        this.creation = creation
        this.lastModified = lastModified
        this.organizer = organizer
        this.modificationId = modificationId
        this.witnesses = HashSet(witnesses)
        this.modificationSignatures = ArrayList()
        if (modificationSignatures != null) {
            this.modificationSignatures.addAll(modificationSignatures)
        }
    }

    override val `object`: String
        get() = Objects.LAO.`object`
    override val action: String
        get() = Action.STATE.action

    fun getWitnesses(): Set<PublicKey> {
        return HashSet(witnesses)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val stateLao = other as StateLao
        return creation == stateLao.creation && lastModified == stateLao.lastModified &&
                id == stateLao.id && name == stateLao.name && organizer == stateLao.organizer &&
                getWitnesses() == stateLao.getWitnesses()
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(
                id, name, creation, lastModified, organizer, getWitnesses())
    }

    override fun toString(): String {
        return ("StateLao{"
                + "id='"
                + id
                + '\''
                + ", name='"
                + name
                + '\''
                + ", creation="
                + creation
                + ", lastModified="
                + lastModified
                + ", organizer='"
                + organizer
                + '\''
                + ", modificationId='"
                + modificationId
                + '\''
                + ", witnesses="
                + witnesses.toTypedArray().contentToString()
                + ", modificationSignatures="
                + modificationSignatures.toTypedArray().contentToString()
                + '}')
    }

    fun getModificationSignatures(): List<PublicKeySignaturePair> {
        return ArrayList(modificationSignatures)
    }
}