package com.github.dedis.popstellar.model.network.method

import java.util.Arrays
import java.util.Collections

/** Enumerate the different low level messages' method  */
enum class Method
/**
 * Constructor for the Method
 *
 * @param method the name of the method
 * @param dataClass the data class (publish/broadcast/catchup/subscribe/unsubscribe)
 * @param expectResult the expect result as a boolean
 */(
        /** Returns the name of the Method.  */
        val method: String,
        /** Returns the data class of the Method.  */
        @JvmField val dataClass: Class<out Message>, private val expectResult: Boolean) {
    SUBSCRIBE("subscribe", Subscribe::class.java, true),
    UNSUBSCRIBE("unsubscribe", Unsubscribe::class.java, true),
    PUBLISH("publish", Publish::class.java, true),
    MESSAGE("broadcast", Broadcast::class.java, false),
    CATCHUP("catchup", Catchup::class.java, true);

    /** Returns the expected result of the Method.  */
    fun expectResult(): Boolean {
        return expectResult
    }

    companion object {
        private val ALL = Collections.unmodifiableList(Arrays.asList(*values()))

        /**
         * Find a given Method
         *
         * @param searched the searched method
         * @return the corresponding enum method
         */
        @JvmStatic
        fun find(searched: String): Method? {
            return ALL.stream().filter { method: Method? -> method!!.method == searched }.findFirst().orElse(null)
        }
    }
}