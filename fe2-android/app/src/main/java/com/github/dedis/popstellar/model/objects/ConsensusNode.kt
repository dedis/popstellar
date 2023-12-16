package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.util.Collections
import java.util.Optional

/** Class representing a Node for consensus.  */
class ConsensusNode : Copyable<ConsensusNode?> {
    @JvmField
    val publicKey: PublicKey?

    // contains messageId of Elect that this node has accepted
    private val acceptedMessageIds: MutableSet<MessageID?>

    // list of all elect instances that this node has created
    private val electInstances: MutableSet<ElectInstance>

    constructor(publicKey: PublicKey?) {
        this.publicKey = publicKey
        electInstances = HashSet()
        acceptedMessageIds = HashSet()
    }

    constructor(consensusNode: ConsensusNode) {
        publicKey = consensusNode.publicKey
        acceptedMessageIds = consensusNode.acceptedMessageIds
        // TODO adapt the codebase so this isn't used by reference

        //
        // consensusNode.acceptedMessageIds.stream().map(MessageID::new).collect(Collectors.toSet());
        electInstances = consensusNode.electInstances
        // TODO adapt the codebase so this isn't used by reference
        //
        // consensusNode.electInstances.stream().map(ElectInstance::new).collect(Collectors.toSet());
    }

    /**
     * Get a set of messageId of Elect that this node has accepted.
     *
     * @return the set of accepted Elect messageId
     */
    fun getAcceptedMessageIds(): Set<MessageID?> {
        return Collections.unmodifiableSet(acceptedMessageIds)
    }

    /**
     * Get the latest ElectInstance that this node has created for the given instanceId.
     *
     * @param instanceId the id of the consensus
     * @return an Optional ElectInstance
     */
    fun getLastElectInstance(instanceId: String): Optional<ElectInstance> {
        // get the latest ElectInstance for the given instanceId
        return electInstances.stream()
                .filter { elect: ElectInstance -> elect.instanceId == instanceId }
                .max(Comparator.comparingLong { obj: ElectInstance -> obj.creation })
    }

    /**
     * Get the state of this node for the latest ElectInstance that this node has created for the
     * given instanceId. If empty, it will return WAITING.
     *
     * @param instanceId the id of the consensus
     * @return the current state
     */
    fun getState(instanceId: String): ElectInstance.State? {
        val lastElect = getLastElectInstance(instanceId)
        return lastElect.map { obj: ElectInstance -> obj.state }.orElse(ElectInstance.State.WAITING)
    }

    /**
     * Add the given ElectInstance to this node. If it was already present, do nothing.
     *
     * @param electInstance the ElectInstance to add
     */
    fun addElectInstance(electInstance: ElectInstance) {
        val messageId = electInstance.messageId
        // if the ElectInstance is not present, add it
        if (electInstances.stream().map { obj: ElectInstance -> obj.messageId }.noneMatch { o: MessageID? -> messageId == o }) {
            electInstances.add(electInstance)
        }
    }

    /**
     * Add the given messageId to the list of accepted Elect.
     *
     * @param electMessageId the messageId of the Elect to add
     */
    fun addMessageIdOfAnAcceptedElect(electMessageId: MessageID?) {
        acceptedMessageIds.add(electMessageId)
    }

    override fun copy(): ConsensusNode {
        return ConsensusNode(this)
    }

    override fun toString(): String {
        return String.format(
                "ConsensusNode{publicKey='%s', acceptedMessageIds='%s', electInstances='%s'}",
                publicKey!!.encoded, acceptedMessageIds, electInstances.toString())
    }
}