package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.utility.security.Hash
import java.util.Objects

@Immutable
class Meeting(
        @JvmField val id: String,
        val name: String,
        @JvmField val creation: Long,
        private val start: Long,
        private val end: Long,
        @JvmField val location: String,
        @JvmField val lastModified: Long,
        @JvmField val modificationId: String,
        modificationSignatures: List<String>?) : Event() {
    @JvmField
    val modificationSignatures: List<String>

    init {
        this.modificationSignatures = modificationSignatures?.let { ArrayList(it) }!!
    }

    override fun getName(): String {
        return name
    }

    override fun getStartTimestamp(): Long {
        return start
    }

    override fun getType(): EventType {
        return EventType.MEETING
    }

    override fun getEndTimestamp(): Long {
        return if (end == 0L) {
            Long.MAX_VALUE
        } else end
    }

    override fun getState(): EventState {
        // This info is just used to display the correct text in the event list

        // The meeting is considered closed when the end time is in the past
        if (isEndPassed) {
            return EventState.CLOSED
        }
        // Open if still not closed and start has passed
        return if (isStartPassed) {
            EventState.OPENED
        } else EventState.CREATED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val meeting = other as Meeting
        return creation == meeting.creation && start == meeting.start && end == meeting.end &&
                lastModified == meeting.lastModified && id == meeting.id && name == meeting.name
                && location == meeting.location && modificationId == meeting.modificationId
                && modificationSignatures == meeting.modificationSignatures
    }

    override fun hashCode(): Int {
        return Objects.hash(
                id,
                name,
                creation,
                start,
                end,
                location,
                lastModified,
                modificationId,
                modificationSignatures)
    }

    override fun toString(): String {
        return ("Meeting{"
                + "id='"
                + id
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
                + ", location='"
                + location
                + '\''
                + ", lastModified="
                + lastModified
                + ", modificationId='"
                + modificationId
                + '\''
                + ", modificationSignatures='"
                + modificationSignatures
                + '\''
                + '}')
    }

    companion object {
        /**
         * Generate the id for dataCreateMeeting. [Ref](https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateMeeting.json)
         *
         * @param laoId ID of the LAO
         * @param creation creation time of Meeting
         * @param name name of Meeting
         * @return the ID of CreateMeeting computed as Hash('M'||lao_id||creation||name)
         */
        @JvmStatic
        fun generateCreateMeetingId(laoId: String?, creation: Long, name: String?): String {
            return Hash.hash(EventType.MEETING.suffix, laoId, creation.toString(), name)
        }

        /**
         * Generate the id for dataStateMeeting. [Ref](https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataStateMeeting.json)
         *
         * @param laoId ID of the LAO
         * @param creation creation time of Meeting
         * @param name name of Meeting
         * @return the ID of StateMeeting computed as Hash('M'||lao_id||creation||name)
         */
        @JvmStatic
        fun generateStateMeetingId(laoId: String?, creation: Long, name: String?): String {
            return Hash.hash(EventType.MEETING.suffix, laoId, creation.toString(), name)
        }
    }
}