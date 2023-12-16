package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.security.Hash
import java.util.Collections
import java.util.Objects
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function

/** Class modeling a Local Autonomous Organization (LAO)  */
class Lao : Copyable<Lao?> {
    @JvmField
    val channel: Channel
    var id: String?
    var name: String? = null

    @JvmField
    var lastModified: Long? = null

    @JvmField
    var creation: Long? = null
    var organizer: PublicKey? = null

    @JvmField
    var modificationId: MessageID? = null
    var pendingUpdates: MutableSet<PendingUpdate?>
    val messageIdToElectInstance: MutableMap<MessageID?, ElectInstance?>
    val keyToNode: Map<PublicKey?, ConsensusNode>?

    constructor(id: String?) {
        requireNotNull(id) { " The id is null" }
        require(id.isNotEmpty()) { " The id of the Lao is empty" }
        channel = Channel.getLaoChannel(id)
        this.id = id
        keyToNode = HashMap()
        messageIdToElectInstance = HashMap()
        pendingUpdates = HashSet()
    }

    constructor(name: String?, organizer: PublicKey, creation: Long) : this(generateLaoId(organizer, creation, name)) {
        // This will throw an exception if name is null or empty
        this.name = name
        this.organizer = organizer
        this.creation = creation
    }

    constructor(
            channel: Channel,
            id: String?,
            name: String?,
            lastModified: Long?,
            creation: Long?,
            organizer: PublicKey?,
            modificationId: MessageID?,
            pendingUpdates: Set<PendingUpdate?>?,
            messageIdToElectInstance: Map<MessageID?, ElectInstance?>?,
            keyToNode: Map<PublicKey?, ConsensusNode?>?) {
        this.channel = channel
        this.id = id
        this.name = name
        this.lastModified = lastModified
        this.creation = creation
        this.organizer = organizer
        this.modificationId = modificationId
        this.pendingUpdates = pendingUpdates?.let { HashSet(it) }!!
        this.messageIdToElectInstance = messageIdToElectInstance?.let { HashMap(it) }!!
        this.keyToNode = Copyable.copy(keyToNode)
    }

    /**
     * Copy constructor
     *
     * @param lao the lao to be deep copied in a new object
     */
    constructor(lao: Lao) {
        channel = lao.channel
        id = lao.id
        name = lao.name
        lastModified = lao.lastModified
        creation = lao.creation
        organizer = lao.organizer
        modificationId = lao.modificationId
        pendingUpdates = HashSet(lao.pendingUpdates)
        // FIXME We need to keep the ElectInstance because the current consensus relies on references
        // (Gabriel Fleischer 11.08.22)
        messageIdToElectInstance = HashMap(lao.messageIdToElectInstance)
        keyToNode = Copyable.copy(lao.keyToNode)
    }

    /**
     * Store the given ElectInstance and update all nodes concerned by it.
     *
     * @param electInstance the ElectInstance
     */
    fun updateElectInstance(electInstance: ElectInstance) {
        val messageId = electInstance.messageId
        messageIdToElectInstance[messageId] = electInstance
        val acceptorsToMessageId = electInstance.acceptorsToMessageId
        // add to each node the messageId of the Elect if they accept it
        keyToNode!!.forEach { (key: PublicKey?, node: ConsensusNode) ->
            if (acceptorsToMessageId.containsKey(key)) {
                node.addMessageIdOfAnAcceptedElect(messageId)
            }
        }

        // add the ElectInstance to the proposer node
        val proposer = keyToNode[electInstance.proposer]
        proposer?.addElectInstance(electInstance)
    }

    fun getElectInstance(messageId: MessageID?): Optional<ElectInstance> {
        return Optional.ofNullable(messageIdToElectInstance[messageId])
    }

    fun getPendingUpdates(): Set<PendingUpdate?> {
        return pendingUpdates
    }

    fun getOrganizer(): PublicKey? {
        return organizer
    }

    fun getId(): String? {
        return id
    }

    fun setId(id: String?) {
        requireNotNull(id) { "The Id of the Lao is null" }
        require(id.isNotEmpty()) { "The id of the Lao is empty" }
        this.id = id
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String?) {
        requireNotNull(name) { " The name of the Lao is null" }
        require(name.isNotEmpty()) { " The name of the Lao is empty" }
        this.name = name
    }

    fun setOrganizer(organizer: PublicKey?) {
        this.organizer = organizer
        keyToNode.computeIfAbsent(organizer, Function<PublicKey?, ConsensusNode> { publicKey: PublicKey? -> ConsensusNode(publicKey) })
    }

    fun initKeyToNode(witnesses: Set<PublicKey?>?) {
        requireNotNull(witnesses) { "The witnesses set is null" }
        for (witness in witnesses) {
            requireNotNull(witness) { "One of the witnesses in the set is null" }
        }
        witnesses.forEach(Consumer<PublicKey?> { w: PublicKey? -> keyToNode.computeIfAbsent(w, Function<PublicKey?, ConsensusNode> { publicKey: PublicKey? -> ConsensusNode(publicKey) }) })
    }

    fun addPendingUpdate(pendingUpdate: PendingUpdate?) {
        pendingUpdates.add(pendingUpdate)
    }

    fun setPendingUpdates(pendingUpdates: MutableSet<PendingUpdate?>) {
        this.pendingUpdates = pendingUpdates
    }

    val nodes: List<ConsensusNode>
        /**
         * Get the list of all nodes of this Lao sorted by the base64 representation of their public key.
         *
         * @return a sorted List of ConsensusNode
         */
        get() {
            val nodes: List<ConsensusNode> = ArrayList(keyToNode!!.values)
            nodes.sort(Comparator.comparing { node: ConsensusNode -> node.publicKey.encoded })
            return nodes
        }

    fun getNode(key: PublicKey): ConsensusNode? {
        return keyToNode!![key]
    }

    fun getMessageIdToElectInstance(): Map<MessageID?, ElectInstance?> {
        return Collections.unmodifiableMap(messageIdToElectInstance)
    }

    override fun copy(): Lao {
        return Lao(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val lao = other as Lao
        return channel == lao.channel && id == lao.id && name == lao.name
                && lastModified == lao.lastModified && creation == lao.creation
                && organizer == lao.organizer && modificationId == lao.modificationId
                && pendingUpdates == lao.pendingUpdates && messageIdToElectInstance == lao.messageIdToElectInstance
                && keyToNode == lao.keyToNode
    }

    override fun hashCode(): Int {
        return Objects.hash(
                channel,
                id,
                name,
                lastModified,
                creation,
                organizer,
                modificationId,
                pendingUpdates,
                messageIdToElectInstance,
                keyToNode)
    }

    override fun toString(): String {
        return ("Lao{"
                + "name='"
                + name
                + '\''
                + ", id='"
                + id
                + '\''
                + ", channel='"
                + channel
                + '\''
                + ", creation="
                + creation
                + ", organizer='"
                + organizer
                + '\''
                + ", lastModified="
                + lastModified
                + ", modificationId='"
                + modificationId
                + '\''
                + ", electInstances="
                + messageIdToElectInstance.values
                + ", transactionPerUser="
                + '}')
    }

    companion object {
        val TAG: String = Lao::class.java.simpleName

        /**
         * Generate the id for dataCreateLao and dataUpdateLao.
         * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateLao.json
         * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataUpdateLao.json
         *
         * @param organizer ID of the organizer
         * @param creation creation time of the LAO
         * @param name original or updated name of the LAO
         * @return the ID of CreateLao or UpdateLao computed as Hash(organizer||creation||name)
         */
        @JvmStatic
        fun generateLaoId(organizer: PublicKey, creation: Long, name: String?): String {
            return Hash.hash(organizer.encoded, creation.toString(), name)
        }
    }
}