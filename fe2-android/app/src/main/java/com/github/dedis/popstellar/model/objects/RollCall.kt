package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.security.Hash
import java.util.Objects

@Immutable
class RollCall(
        @JvmField val id: String,
        @JvmField val persistentId: String,
        val name: String,
        @JvmField val creation: Long,
        @JvmField val start: Long,
        @JvmField val end: Long,
        val state: EventState,
        @JvmField val attendees: Set<PublicKey>,
        @JvmField val location: String,
        @JvmField val description: String) : Event() {

    override fun getName(): String {
        return name
    }

    override fun getStartTimestamp(): Long {
        return start
    }

    override fun getType(): EventType {
        return EventType.ROLL_CALL
    }

    override fun getEndTimestamp(): Long {
        return if (end == 0L) {
            Long.MAX_VALUE
        } else end
    }

    override fun getState(): EventState {
        return state
    }

    val isClosed: Boolean
        /**
         * @return true if the roll-call is closed, false otherwise
         */
        get() = EventState.CLOSED == state
    val isOpen: Boolean
        /**
         * @return true if the roll-call is currently open, false otherwise
         */
        get() = EventState.OPENED == state

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val rollCall = other as RollCall
        return creation == rollCall.creation && start == rollCall.start && end == rollCall.end &&
                id == rollCall.id && persistentId == rollCall.persistentId && name == rollCall.name
                && state == rollCall.state && attendees == rollCall.attendees &&
                location == rollCall.location && description == rollCall.description
    }

    override fun hashCode(): Int {
        return Objects.hash(
                id, persistentId, name, creation, start, end, state, attendees, location, description)
    }

    override fun toString(): String {
        return ("RollCall{"
                + "id='"
                + id
                + '\''
                + ", persistentId='"
                + persistentId
                + '\''
                + ", name='"
                + name
                + '\''
                + ", creation="
                + creation
                + ", start="
                + start
                + ", end="
                + end
                + ", state="
                + state
                + ", attendees="
                + attendees.toTypedArray().contentToString()
                + ", location='"
                + location
                + '\''
                + ", description='"
                + description
                + '\''
                + '}')
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
            return Hash.hash(EventType.ROLL_CALL.suffix, laoId, java.lang.Long.toString(creation), name)
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
            return Hash.hash(EventType.ROLL_CALL.suffix, laoId, opens, java.lang.Long.toString(openedAt))
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
            return Hash.hash(EventType.ROLL_CALL.suffix, laoId, closes, java.lang.Long.toString(closedAt))
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