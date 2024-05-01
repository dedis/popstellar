package com.github.dedis.popstellar.model.network.method.message.data

import java.util.Collections

/** Enumerates all possible messages objects */
enum class Objects
/**
 * Constructor for a message Object
 *
 * @param object name of the object
 */
(
    /** Returns the name of the Object. */
    val `object`: String
) {
  LAO("lao"),
  MEETING("meeting"),
  MESSAGE("message"),
  ROLL_CALL("roll_call"),
  ELECTION("election"),
  CONSENSUS("consensus"),
  CHIRP("chirp"),
  REACTION("reaction"),
  COIN("coin"),
  POPCHA("popcha"),
  FEDERATION("federation");

  /**
   * Function that tells whether the given object type has to be persisted.
   *
   * @return true if it is going to be saved on disk, false if only in memory
   */
  fun hasToBePersisted(): Boolean {
    return when (`object`) {
      LAO.`object`,
      ELECTION.`object`,
      ROLL_CALL.`object`,
      CHIRP.`object`,
      REACTION.`object`,
      MEETING.`object`,
      COIN.`object`,
      MESSAGE.`object` -> true

      // Consensus, Popcha and Federation are for now the ones excluded from persistence
      else -> false
    }
  }

  companion object {
    private val ALL = values().toList().let(Collections::unmodifiableList)

    /**
     * Find a given Object
     *
     * @param searched the searched object
     * @return the corresponding enum object
     */
    fun find(searched: String): Objects? {
      return ALL.stream()
          .filter { obj: Objects -> obj.`object` == searched }
          .findFirst()
          .orElse(null)
    }
  }
}
