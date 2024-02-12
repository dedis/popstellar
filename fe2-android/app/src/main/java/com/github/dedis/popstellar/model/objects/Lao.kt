package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.util.Objects

/** Class modeling a Local Autonomous Organization (LAO) */
class Lao : Copyable<Lao> {
  val channel: Channel
  var id: String
    set(id) {
      require(id.isNotEmpty()) { "The id of the Lao is empty" }
      field = id
    }

  var pendingUpdates: MutableSet<PendingUpdate>

  var name: String? = null
    private set

  var lastModified: Long = 0
  var creation: Long = 0
  var organizer: PublicKey? = null
  var modificationId: MessageID? = null

  constructor(id: String?) {
    require(!id.isNullOrEmpty()) { " The id is null or empty" }

    this.channel = getLaoChannel(id)
    this.id = id
    this.pendingUpdates = HashSet()
  }

  constructor(createLao: CreateLao) : this(createLao.id) {
    this.name = createLao.name
    this.creation = createLao.creation
    this.lastModified = createLao.creation
    this.organizer = createLao.organizer
  }

  constructor(
      name: String?,
      organizer: PublicKey,
      creation: Long
  ) : this(generateLaoId(organizer, creation, name)) {
    // This will throw an exception if name is null or empty
    this.name = name
    this.organizer = organizer
    this.creation = creation
  }

  constructor(
      channel: Channel,
      id: String,
      name: String,
      lastModified: Long?,
      creation: Long,
      organizer: PublicKey,
      modificationId: MessageID?,
      pendingUpdates: Set<PendingUpdate>
  ) {
    this.channel = channel
    this.id = id
    this.name = name
    this.lastModified = lastModified ?: creation
    this.creation = creation
    this.organizer = organizer
    this.modificationId = modificationId
    this.pendingUpdates = HashSet(pendingUpdates)
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
  }

  fun setName(name: String?) {
    requireNotNull(name) { "The name of the Lao is empty" }
    require(name.isNotEmpty()) { "The name of the Lao is empty" }

    this.name = name
  }

  fun addPendingUpdate(pendingUpdate: PendingUpdate) {
    pendingUpdates.add(pendingUpdate)
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
    return channel == lao.channel &&
        id == lao.id &&
        name == lao.name &&
        lastModified == lao.lastModified &&
        creation == lao.creation &&
        organizer == lao.organizer &&
        modificationId == lao.modificationId &&
        pendingUpdates == lao.pendingUpdates
  }

  override fun hashCode(): Int {
    return Objects.hash(
        channel, id, name, lastModified, creation, organizer, modificationId, pendingUpdates)
  }

  override fun toString(): String {
    return "Lao{name='$name', id='$id', channel='$channel', creation=$creation, organizer='$organizer', " +
        "lastModified=$lastModified, modificationId='$modificationId'}"
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
      return hash(organizer.encoded, creation.toString(), name)
    }
  }
}
