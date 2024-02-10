package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import java.util.Objects

@Immutable
class RollCall(
    val id: String,
    val persistentId: String,
    override val name: String,
    val creation: Long,
    override val startTimestamp: Long,
    val end: Long,
    override val state: EventState,
    val attendees: MutableSet<PublicKey>,
    val location: String,
    val description: String
) : Event() {

  override val type: EventType
    get() = EventType.ROLL_CALL

  override val endTimestamp: Long
    get() =
        if (end == 0L) {
          Long.MAX_VALUE
        } else end

  val isClosed: Boolean
    /** @return true if the roll-call is closed, false otherwise */
    get() = EventState.CLOSED == state

  val isOpen: Boolean
    /** @return true if the roll-call is currently open, false otherwise */
    get() = EventState.OPENED == state

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val rollCall = other as RollCall
    return creation == rollCall.creation &&
        startTimestamp == rollCall.startTimestamp &&
        end == rollCall.end &&
        id == rollCall.id &&
        persistentId == rollCall.persistentId &&
        name == rollCall.name &&
        state === rollCall.state &&
        attendees == rollCall.attendees &&
        location == rollCall.location &&
        description == rollCall.description
  }

  override fun hashCode(): Int {
    return Objects.hash(
        id,
        persistentId,
        name,
        creation,
        startTimestamp,
        end,
        state,
        attendees,
        location,
        description)
  }

  override fun toString(): String {
    return "RollCall{id='$id', persistentId='$persistentId', name='$name', creation=$creation, start=$startTimestamp, end=$end, state=$state, attendees=${
      attendees.toTypedArray().contentToString()
    }, location='$location', description='$description'}"
  }

  companion object {
    /**
     * Generate the id for dataCreateRollCall.
     * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateRollCall.json
     *
     * @param laoId ID of the LAO
     * @param creation creation time of RollCall
     * @param name name of RollCall
     * @return the ID of CreateRollCall computed as Hash('R'||lao_id||creation||name)
     */
    @JvmStatic
    fun generateCreateRollCallId(laoId: String?, creation: Long, name: String?): String {
      return hash(EventType.ROLL_CALL.suffix, laoId, creation.toString(), name)
    }

    /**
     * Generate the id for dataOpenRollCall.
     * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataOpenRollCall.json
     *
     * @param laoId ID of the LAO
     * @param opens id of RollCall to open
     * @param openedAt open time of RollCall
     * @return the ID of OpenRollCall computed as Hash('R'||lao_id||opens||opened_at)
     */
    fun generateOpenRollCallId(laoId: String?, opens: String?, openedAt: Long): String {
      return hash(EventType.ROLL_CALL.suffix, laoId, opens, openedAt.toString())
    }

    /**
     * Generate the id for dataCloseRollCall.
     * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCloseRollCall.json
     *
     * @param laoId ID of the LAO
     * @param closes id of RollCall to close
     * @param closedAt closing time of RollCall
     * @return the ID of CloseRollCall computed as Hash('R'||lao_id||closes||closed_at)
     */
    fun generateCloseRollCallId(laoId: String?, closes: String?, closedAt: Long): String {
      return hash(EventType.ROLL_CALL.suffix, laoId, closes, closedAt.toString())
    }

    @JvmStatic
    fun openRollCall(rollCall: RollCall): RollCall {
      return setRollCallState(rollCall, EventState.OPENED)
    }

    @JvmStatic
    fun closeRollCall(rollCall: RollCall): RollCall {
      return setRollCallState(rollCall, EventState.CLOSED)
    }

    private fun setRollCallState(rollCall: RollCall, state: EventState): RollCall {
      return RollCallBuilder(rollCall).setState(state).build()
    }
  }
}
