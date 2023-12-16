package com.github.dedis.popstellar.model.network.method.message.data

import java.util.Collections

/** Enumerates all possible messages objects  */
enum class Objects
/**
 * Constructor for a message Object
 *
 * @param object name of the object
 */(
        /** Returns the name of the Object.  */
        @JvmField val `object`: String) {
    LAO("lao"),
    MEETING("meeting"),
    MESSAGE("message"),
    ROLL_CALL("roll_call"),
    ELECTION("election"),
    CONSENSUS("consensus"),
    CHIRP("chirp"),
    REACTION("reaction"),
    COIN("coin"),
    POPCHA("popcha");

    /**
     * Function that tells whether the given object type has to be persisted.
     *
     * @return true if it is going to be saved on disk, false if only in memory
     */
    fun hasToBePersisted(): Boolean {
        return when (`object`) {
            "lao", "election", "roll_call", "chirp", "reaction", "meeting", "coin", "message" -> true
            "consensus", "popcha" -> false
            else -> false
        }
    }

    companion object {
        private val ALL = Collections.unmodifiableList(listOf(*values()))

        /**
         * Find a given Object
         *
         * @param searched the searched object
         * @return the corresponding enum object
         */
        @JvmStatic
        fun find(searched: String): Objects? {
            return ALL.stream()
                    .filter { `object`: Objects? -> `object`!!.`object` == searched }
                    .findFirst()
                    .orElse(null)
        }
    }
}