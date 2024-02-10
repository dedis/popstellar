package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import java.io.Serializable
import java.util.Collections
import java.util.Objects
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * This class represent a channel used to subscribe to a certain part of the messages. More detailed
 * information are given in the protocol specification of the application.
 *
 * It is implemented in a functional manner to avoid any unintended modification
 */
@Immutable
class Channel : Serializable {
  // ================ Object itself =================
  // This list contains the different parts of the channel except the root as every channel has a
  // root part
  // So for the channel /root/lao_id/election_id, the elements will be [lao_id, election_id]
  private val segments: List<String>

  private constructor(vararg segments: String) {
    this.segments = segments.toList()

    require(CHANNEL_VALIDATOR.test(asString)) { "$asString is not a valid channel" }
  }

  private constructor(base: Channel, subSegments: String) {
    val newSegments: MutableList<String> = ArrayList(base.segments)
    newSegments.add(subSegments)
    segments = Collections.unmodifiableList(newSegments)

    require(CHANNEL_VALIDATOR.test(asString)) { "$asString is not a valid channel" }
  }

  /**
   * Return a channel that is this channel with one more element underneath
   *
   * @param segment to append to this channel
   * @return a new Channel object
   */
  fun subChannel(segment: String): Channel {
    return Channel(this, segment)
  }

  val isLaoChannel: Boolean
    /** @return true if this channel is of the form '/root/lao_id' */
    get() = segments.size == 1

  val isElectionChannel: Boolean
    /**
     * Check if the channel could be an election channel.
     *
     * This check is not perfect, any channel of the form /root/xxx/xxx are accepted as valid
     * election channel
     *
     * Be careful when using this function
     *
     * @return true if this channel could be an election channel
     */
    get() = segments.size == 2

  /** @return the LAO ID contained in this channel */
  fun extractLaoId(): String {
    check(segments.isNotEmpty()) {
      "This channel is the root channel and does not contain any LAO ID"
    }
    // In the current implementation of the protocol, the first element of the channel (if present)
    // is always the LAO id
    return segments[0]
  }

  /**
   * Retrieve the election id contained in this channel
   *
   * Warning, there is no guarantee that the retrieved value is in fact an election id. This will
   * only work if the function is performed on an election channel
   *
   * @return the election id contained in this channel
   */
  fun extractElectionId(): String {
    check(segments.size >= 2) { "This channel is not an election channel" }

    return segments[1]
  }

  val asString: String
    /** @return the protocol like String representation of the channel (/root/abc/efg) */
    get() =
        ROOT_CHANNEL + segments.stream().map { e: String -> "/$e" }.collect(Collectors.joining())

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val channel = other as Channel
    return segments == channel.segments
  }

  override fun hashCode(): Int {
    return Objects.hash(segments)
  }

  override fun toString(): String {
    return "Channel{'$asString'}"
  }

  companion object {
    private const val ROOT_CHANNEL = "/root"
    private const val DELIMITER = "/"

    // Predicate matching a String with the protocol regex of a channel
    private val CHANNEL_VALIDATOR =
        Pattern.compile("^/root(/[a-zA-Z0-9=!*+()~?#%_-]+)*+$").asPredicate()

    /** Root channel, every client is subscribed to it. */
    @JvmField val ROOT = Channel()

    private const val serialVersionUID = 1L

    /**
     * Create a Channel from the protocol string representation of it
     *
     * @param value of the channel
     * @return the new channel
     */
    @JvmStatic
    @Suppress("SpreadOperator")
    fun fromString(value: String): Channel {
      // Remove the "/root/" part of the value as it is not relevant and does not need to be stored
      // Check first that value is not simply root
      if (value == ROOT_CHANNEL) {
        return ROOT
      }

      val relevantPart = value.substring(ROOT_CHANNEL.length + 1)
      return Channel(
          *relevantPart.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    /**
     * Create a new LAO Channel (/root/<lao_id>)
     *
     * @param laoId of the lao
     * @return the new channel
     */
    @JvmStatic
    fun getLaoChannel(laoId: String): Channel {
      return ROOT.subChannel(laoId)
    }
  }
}
