package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.util.Collections
import java.util.Objects

/**
 * Class holding information of a ConsensusElect message and its current states including the
 * key/messageId of every node that have accepted this Elect with an ElectAccept.
 */
class ElectInstance : Copyable<ElectInstance> {
  val messageId: MessageID
  val channel: Channel
  val proposer: PublicKey
  private val elect: ConsensusElect
  val nodes: Set<PublicKey>

  // map the public key of acceptors to the id of their ElectAccept message
  private val acceptorToMessageId: MutableMap<PublicKey, MessageID>

  var state: State

  constructor(
      messageId: MessageID,
      channel: Channel,
      proposer: PublicKey,
      nodes: Set<PublicKey>,
      elect: ConsensusElect
  ) {
    this.messageId = messageId
    this.channel = channel
    this.proposer = proposer
    this.elect = elect
    this.nodes = Collections.unmodifiableSet(nodes)
    this.acceptorToMessageId = HashMap()
    this.state = State.STARTING
  }

  constructor(electInstance: ElectInstance) {
    messageId = electInstance.messageId
    channel = electInstance.channel
    proposer = electInstance.proposer
    elect = electInstance.elect
    nodes = electInstance.nodes
    acceptorToMessageId = HashMap(electInstance.acceptorToMessageId)
    state = electInstance.state
  }

  val instanceId: String
    get() = elect.instanceId

  val key: ConsensusKey
    get() = elect.key

  val value: Any
    get() = elect.value

  val creation: Long
    get() = elect.creation

  val acceptorsToMessageId: Map<PublicKey, MessageID>
    get() = Collections.unmodifiableMap(acceptorToMessageId)

  fun addElectAccept(
      publicKey: PublicKey,
      messageId: MessageID,
      electAccept: ConsensusElectAccept
  ) {
    if (electAccept.isAccept) {
      acceptorToMessageId[publicKey] = messageId
    }
  }

  override fun toString(): String {
    return "ElectInstance{messageId='${messageId.encoded}', instanceId='$instanceId', channel='$channel', " +
        "proposer='${proposer.encoded}', elect=$elect, nodes=$nodes, state=$state}"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ElectInstance
    return messageId == that.messageId &&
        channel == that.channel &&
        proposer == that.proposer &&
        elect == that.elect &&
        nodes == that.nodes &&
        acceptorToMessageId == that.acceptorToMessageId &&
        state == that.state
  }

  override fun hashCode(): Int {
    return Objects.hash(messageId, channel, proposer, elect, nodes, acceptorToMessageId, state)
  }

  override fun copy(): ElectInstance {
    return ElectInstance(this)
  }

  enum class State {
    FAILED,
    WAITING,
    STARTING,
    ACCEPTED
  }

  companion object {
    /**
     * Generate the id for a consensus instance. This instanceId is used to group all Elect that
     * refers to the same object and property and will be used in every Consensus Data message.
     *
     * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataElect.json
     *
     * @param type The object type that the consensus refers to
     * @param id The object id that the consensus refers to
     * @param property The property of the object that the value refers to
     * @return the id computed as HashLen('consensus', key:type, key:id, key:property)
     */
    @JvmStatic
    fun generateConsensusId(type: String, id: String, property: String): String {
      return hash("consensus", type, id, property)
    }
  }
}
