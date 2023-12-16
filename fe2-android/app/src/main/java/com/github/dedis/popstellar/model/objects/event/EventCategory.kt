package com.github.dedis.popstellar.model.objects.event

/** Enum class modeling the Event Categories  */
enum class EventCategory {
    PAST {
        override fun toString(): String {
            return "Past Events"
        }
    },
    PRESENT {
        override fun toString(): String {
            return "Present Events"
        }
    },
    FUTURE {
        override fun toString(): String {
            return "Future Events"
        }
    }
}