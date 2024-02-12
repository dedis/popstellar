package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.ConsensusNode
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.Collections
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsensusRepository @Inject constructor() {
  private val consensusByLao: MutableMap<String, LaoConsensus> = HashMap()
  private val channelToNodesSubject: MutableMap<Channel, BehaviorSubject<List<ConsensusNode>>> =
      HashMap()

  /**
   * Return an Observable to the list of nodes in a given channel.
   *
   * @param channel the lao channel.
   * @return an Observable to the list of nodes
   */
  fun getNodesByChannel(channel: Channel): Observable<List<ConsensusNode>> {
    return channelToNodesSubject[channel] ?: Observable.empty()
  }

  /**
   * Emit an update to the observer of nodes for the given lao channel. Create the BehaviorSubject
   * if absent (first update).
   *
   * @param channel the lao channel
   */
  fun updateNodesByChannel(channel: Channel) {
    val nodes: List<ConsensusNode> = getLaoConsensus(channel.extractLaoId()).nodes
    channelToNodesSubject.putIfAbsent(channel, BehaviorSubject.create())
    channelToNodesSubject.getValue(channel).onNext(nodes)
  }

  fun getNodes(laoId: String): List<ConsensusNode> {
    return getLaoConsensus(laoId).nodes
  }

  fun updateElectInstanceByLao(laoId: String, electInstance: ElectInstance) {
    getLaoConsensus(laoId).updateElectInstance(electInstance)
  }

  fun getElectInstance(laoId: String, messageId: MessageID): Optional<ElectInstance> {
    // TODO uncomment that when consensus does not rely on call by reference
    //    Optional<ElectInstance> optional = getLaoConsensus(laoId).getElectInstance(messageId);
    //    return optional.map(ElectInstance::new); // If empty returns empty optional, if not
    //    returns optional with copy of retrieved ElectInstance
    return getLaoConsensus(laoId).getElectInstance(messageId)
  }

  fun setOrganizer(laoId: String, organizer: PublicKey) {
    getLaoConsensus(laoId).setOrganizer(organizer)
  }

  fun initKeyToNode(laoId: String, witnesses: Set<PublicKey?>?) {
    getLaoConsensus(laoId).initKeyToNode(witnesses)
  }

  fun getNodeByLao(laoId: String, key: PublicKey): ConsensusNode? {
    return getLaoConsensus(laoId).getNode(key)
  }

  fun getMessageIdToElectInstanceByLao(laoId: String): Map<MessageID, ElectInstance> {
    return getLaoConsensus(laoId).getMessageIdToElectInstance()
  }

  /** Get in a thread-safe fashion the consensus object for the lao, computes it if absent. */
  @Synchronized
  private fun getLaoConsensus(laoId: String): LaoConsensus {
    // Create the lao consensus object if it is not present yet
    return consensusByLao.computeIfAbsent(laoId) { LaoConsensus() }
  }

  private class LaoConsensus {
    private val messageIdToElectInstance = ConcurrentHashMap<MessageID, ElectInstance>()
    private val keyToNode = ConcurrentHashMap<PublicKey, ConsensusNode>()

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
      keyToNode.forEach { (key: PublicKey, node: ConsensusNode) ->
        if (acceptorsToMessageId.containsKey(key)) {
          node.addMessageIdOfAnAcceptedElect(messageId)
        }
      }

      // add the ElectInstance to the proposer node
      val proposer = keyToNode[electInstance.proposer]
      proposer?.addElectInstance(electInstance)
    }

    fun getElectInstance(messageId: MessageID): Optional<ElectInstance> {
      return Optional.ofNullable(messageIdToElectInstance[messageId])
    }

    fun setOrganizer(organizer: PublicKey) {
      keyToNode.computeIfAbsent(organizer) { publicKey: PublicKey -> ConsensusNode(publicKey) }
    }

    val nodes: List<ConsensusNode>
      /**
       * Get the list of all nodes of this Lao sorted by the base64 representation of their public
       * key.
       *
       * @return a sorted List of ConsensusNode
       */
      get() {
        val nodes: MutableList<ConsensusNode> = ArrayList(keyToNode.values)
        nodes.sortWith(Comparator.comparing { node: ConsensusNode -> node.publicKey.encoded })
        return nodes
      }

    fun initKeyToNode(witnesses: Set<PublicKey?>?) {
      requireNotNull(witnesses) { "The witnesses set is null" }
      for (witness in witnesses) {
        requireNotNull(witness) { "One of the witnesses in the set is null" }
      }
      witnesses.forEach(
          Consumer { w: PublicKey? ->
            keyToNode.computeIfAbsent(w!!) { publicKey: PublicKey -> ConsensusNode(publicKey) }
          })
    }

    fun getNode(key: PublicKey): ConsensusNode? {
      return keyToNode[key]
    }

    fun getMessageIdToElectInstance(): Map<MessageID, ElectInstance> {
      return Collections.unmodifiableMap(messageIdToElectInstance)
    }
  }
}
