package com.github.dedis.popstellar.model.network.method.message.data.meeting

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName
import java.util.Optional

/** Data received to track the state of a meeting */
class StateMeeting(
    laoId: String,
    id: String,
    name: String,
    creation: Long,
    lastModified: Long,
    location: String?,
    start: Long,
    end: Long,
    modificationId: String,
    modificationSignatures: List<String>
) : Data() {
  val id: String
  val name: String
  val creation: Long

  @SerializedName("last_modified") val lastModified: Long
  private val location: String?

  val start: Long
  val end: Long

  @SerializedName("modification_id") val modificationId: String

  @SerializedName("modification_signatures")
  val modificationSignatures: List<String>
    get() = ArrayList(field)

  /**
   * Constructor for a data State Meeting Event
   *
   * @param laoId of the LAO
   * @param id of the state Meeting message, Hash("M"||laoId||creation||name)
   * @param name name of the Meeting
   * @param creation time of creation
   * @param lastModified time of the last modification
   * @param location location of the Meeting
   * @param start of the Meeting
   * @param end of the Meeting
   * @param modificationId id of the modification (either creation/update)
   * @param modificationSignatures signatures of the witnesses on the modification message
   * @throws IllegalArgumentException if the id is not valid
   */
  init {
    val builder =
        verify()
            .isNotEmptyBase64(laoId, "lao id")
            .validStateMeetingId(id, laoId, creation, name)
            .validPastTimes(creation)
            .orderedTimes(creation, start)

    this.id = id
    this.name = name
    this.creation = creation
    this.lastModified = lastModified
    this.location = location
    this.start = start
    if (end != 0L) {
      builder.orderedTimes(start, end)
      this.end = end
    } else {
      this.end = start + 60 * 60
    }
    this.modificationId = modificationId
    this.modificationSignatures = ArrayList(modificationSignatures)
  }

  fun getLocation(): Optional<String> {
    return Optional.ofNullable(location)
  }

  override val `object`: String
    get() = Objects.MEETING.`object`

  override val action: String
    get() = Action.STATE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as StateMeeting
    return creation == that.creation &&
        lastModified == that.lastModified &&
        start == that.start &&
        end == that.end &&
        id == that.id &&
        name == that.name &&
        getLocation() == that.getLocation() &&
        modificationId == that.modificationId &&
        modificationSignatures == that.modificationSignatures
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(
        id,
        name,
        creation,
        lastModified,
        getLocation(),
        start,
        end,
        modificationId,
        modificationSignatures)
  }

  override fun toString(): String {
    return "StateMeeting{id='$id', name='$name', creation=$creation, lastModified=$lastModified, location='$location', start=$start, end=$end, modificationId='$modificationId', modificationSignatures=${
      modificationSignatures.toTypedArray().contentToString()
    }}"
  }
}
