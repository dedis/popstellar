package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey

/** Builder for the LAO object */
class LaoBuilder {
  private var channel: Channel? = null
  private var id: String? = null
  private var name: String? = null
  private var lastModified: Long? = null
  private var creation: Long = 0
  private var organizer: PublicKey? = null
  private var modificationId: MessageID? = null
  private var pendingUpdates: Set<PendingUpdate>? = HashSet()

  constructor()

  constructor(lao: Lao) {
    channel = lao.channel
    id = lao.id
    name = lao.name
    lastModified = lao.lastModified
    creation = lao.creation
    organizer = lao.organizer
    modificationId = lao.modificationId
    pendingUpdates = lao.pendingUpdates
  }

  fun setChannel(channel: Channel?): LaoBuilder {
    this.channel = channel
    return this
  }

  fun setId(id: String?): LaoBuilder {
    this.id = id
    return this
  }

  fun setName(name: String?): LaoBuilder {
    this.name = name
    return this
  }

  fun setLastModified(lastModified: Long?): LaoBuilder {
    this.lastModified = lastModified
    return this
  }

  fun setCreation(creation: Long): LaoBuilder {
    this.creation = creation
    return this
  }

  fun setOrganizer(organizer: PublicKey?): LaoBuilder {
    this.organizer = organizer
    return this
  }

  fun setModificationId(modificationId: MessageID?): LaoBuilder {
    this.modificationId = modificationId
    return this
  }

  fun setPendingUpdates(pendingUpdates: Set<PendingUpdate>?): LaoBuilder {
    this.pendingUpdates = pendingUpdates
    return this
  }

  fun build(): Lao {
    checkNotNull(channel) { "Channel is null" }
    checkNotNull(organizer) { "Organizer is null" }
    checkNotNull(id) { "Id is null" }
    checkNotNull(name) { "Name is null" }
    checkNotNull(pendingUpdates) { "PendingUpdates is null" }

    return Lao(
        channel!!,
        id!!,
        name!!,
        lastModified,
        creation,
        organizer!!,
        modificationId,
        pendingUpdates!!)
  }
}
