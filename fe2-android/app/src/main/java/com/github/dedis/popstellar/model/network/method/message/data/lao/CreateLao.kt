package com.github.dedis.popstellar.model.network.method.message.data.lao

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.MessageValidator.verify
import java.time.Instant

/** Data sent when creating a new LAO */
@Immutable
class CreateLao : Data {
  val id: String
  val name: String
  val creation: Long
  val organizer: PublicKey
  val witnesses: List<PublicKey>
    get() = ArrayList(field)

  /**
   * Constructor for a Data CreateLao
   *
   * @param id of the LAO creation message, Hash(organizer||creation||name)
   * @param name name of the LAO
   * @param creation time of creation
   * @param organizer id of the LAO's organizer
   * @param witnesses list of witnesses of the LAO
   * @throws IllegalArgumentException if arguments are invalid
   */
  constructor(
      id: String,
      name: String,
      creation: Long,
      organizer: PublicKey,
      witnesses: List<PublicKey>
  ) {
    // Organizer and witnesses are checked to be base64 at deserialization
    verify().validLaoId(id, organizer, creation, name).validPastTimes(creation)

    this.id = id
    this.name = name
    this.creation = creation
    this.organizer = organizer
    this.witnesses = ArrayList(witnesses)
  }

  constructor(name: String, organizer: PublicKey, witnesses: List<PublicKey>) {
    this.name = name
    this.organizer = organizer
    this.creation = Instant.now().epochSecond
    // This checks that name and organizer are not empty or null
    this.id = Lao.generateLaoId(organizer, creation, name)
    this.witnesses = ArrayList(witnesses)
  }

  override val `object`: String
    get() = Objects.LAO.`object`

  override val action: String
    get() = Action.CREATE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val createLao = other as CreateLao
    return creation == createLao.creation &&
        id == createLao.id &&
        name == createLao.name &&
        organizer == createLao.organizer &&
        witnesses == createLao.witnesses
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(id, name, creation, organizer, witnesses)
  }

  override fun toString(): String {
    return "CreateLao{id='$id', name='$name', creation=$creation, organizer='$organizer', witnesses=${
      witnesses.toTypedArray().contentToString()
    }}"
  }
}
