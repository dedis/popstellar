package com.github.dedis.popstellar.model.network.method.message.data.meeting

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.utility.MessageValidator.verify
import java.util.Optional

/** Data sent to create a new meeting */
@Immutable
class CreateMeeting : Data {
  val id: String
  val name: String
  val creation: Long
  private val location: String?
  val start: Long
  val end: Long

  /**
   * Constructor for a data Create Meeting Event
   *
   * @param laoId id of the LAO
   * @param id of the Meeting creation message, Hash("M"||laoId||creation||name)
   * @param name name of the Meeting
   * @param creation time of creation
   * @param location location of the Meeting
   * @param start of the Meeting
   * @param end of the Meeting
   * @throws IllegalArgumentException if the id is invalid
   */
  constructor(
      laoId: String,
      id: String,
      name: String,
      creation: Long,
      location: String?,
      start: Long,
      end: Long
  ) {
    val builder =
        verify()
            .isNotEmptyBase64(laoId, "lao id")
            .validCreateMeetingId(id, laoId, creation, name)
            .validPastTimes(creation)
            .orderedTimes(creation, start)

    this.id = id
    this.name = name
    this.creation = creation
    this.location = location
    this.start = start
    if (end != 0L) {
      builder.orderedTimes(start, end)
      this.end = end
    } else {
      this.end = start + 60 * 60
    }
  }

  constructor(
      laoId: String,
      name: String,
      creation: Long,
      location: String?,
      start: Long,
      end: Long
  ) {
    id = Meeting.generateCreateMeetingId(laoId, creation, name)
    this.name = name
    this.creation = creation
    this.location = location
    this.start = start
    this.end = end
  }

  fun getLocation(): Optional<String> {
    return Optional.ofNullable(location)
  }

  override val `object`: String
    get() = Objects.MEETING.`object`

  override val action: String
    get() = Action.CREATE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as CreateMeeting
    return creation == that.creation &&
        start == that.start &&
        end == that.end &&
        id == that.id &&
        name == that.name &&
        getLocation() == that.getLocation()
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(id, name, creation, getLocation(), start, end)
  }

  override fun toString(): String {
    return "CreateMeeting{id='$id', name='$name', creation=$creation, location='$location', start=$start, end=$end}"
  }
}
