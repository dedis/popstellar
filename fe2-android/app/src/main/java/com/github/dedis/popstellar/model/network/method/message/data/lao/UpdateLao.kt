package com.github.dedis.popstellar.model.network.method.message.data.lao

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName

/** Data sent to update the lao specifications */
@Immutable
class UpdateLao(
    organizer: PublicKey,
    creation: Long,
    name: String,
    lastModified: Long,
    witnesses: Set<PublicKey>
) : Data {
  val id: String
  val name: String

  @SerializedName("last_modified") val lastModified: Long
  val witnesses: Set<PublicKey>
    get() = HashSet(field)

  /**
   * Constructor for a Data UpdateLao
   *
   * @param organizer public key of the LAO
   * @param creation creation time
   * @param name name of the LAO
   * @param lastModified time of last modification
   * @param witnesses list of witnesses of the LAO
   * @throws IllegalArgumentException if arguments are invalid
   */
  init {
    // Witnesses are checked to be base64 at deserialization, but not organizer
    verify()
        .isNotEmptyBase64(organizer.encoded, "organizer")
        .stringNotEmpty(name, "name")
        .orderedTimes(creation, lastModified)
        .validPastTimes(creation, lastModified)

    this.id = Lao.generateLaoId(organizer, creation, name)
    this.name = name
    this.lastModified = lastModified
    this.witnesses = HashSet(witnesses)
  }

  override val `object`: String
    get() = Objects.LAO.`object`

  override val action: String
    get() = Action.UPDATE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val updateLao = other as UpdateLao
    return lastModified == updateLao.lastModified &&
        name == updateLao.name &&
        id == updateLao.id &&
        witnesses == updateLao.witnesses
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(name, lastModified, witnesses)
  }

  override fun toString(): String {
    return "UpdateLao{id='$id', name='$name', lastModified=$lastModified, witnesses=${
      witnesses.toTypedArray().contentToString()
    }}"
  }
}
