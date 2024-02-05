package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.google.gson.annotations.SerializedName

/** Data sent to close a Roll-Call */
@Immutable
/**
 * Constructor for a data Close Roll-Call Event
 *
 * @param laoId id of the LAO
 * @param closes The 'update_id' of the latest roll call open, or in its absence, the 'id' field of
 *   the roll call creation
 * @param closedAt timestamp of the roll call close
 * @param attendees list of attendees of the Roll-Call
 */
class CloseRollCall(
    laoId: String,
    val closes: String,
    @field:SerializedName("closed_at") val closedAt: Long,
    attendees: List<PublicKey>
) : Data {
  @SerializedName("update_id")
  val updateId: String = RollCall.generateCloseRollCallId(laoId, closes, closedAt)

  val attendees: List<PublicKey> = ArrayList(attendees)
    get() = ArrayList(field)

  override val `object`: String
    get() = Objects.ROLL_CALL.`object`

  override val action: String
    get() = Action.CLOSE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as CloseRollCall
    return closedAt == that.closedAt &&
        updateId == that.updateId &&
        closes == that.closes &&
        attendees == that.attendees
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(updateId, closes, closedAt, attendees)
  }

  override fun toString(): String {
    return "CloseRollCall{updateId='$updateId', closes='$closes', closedAt=$closedAt, attendees=${
      attendees.toTypedArray().contentToString()
    }}"
  }
}
