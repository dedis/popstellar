package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class represent a channel used to subscribe to a certain part of the messages. More detailed
 * information are given in the protocol specification of the application.
 *
 * <p>It is implemented in a functional manner to avoid any unintended modification
 */
public class Channel implements Serializable {

  private static final String ROOT_CHANNEL = "/root";
  private static final String DELIMITER = "/";
  // Predicate matching a String with the protocol regex of a channel
  private static final Predicate<String> CHANNEL_VALIDATOR =
      Pattern.compile("^/root(/[a-zA-Z0-9=!*+()~?#%_-]+)*+$").asPredicate();

  /** Root channel, every client is subscribed to it. */
  public static final Channel ROOT = new Channel();

  /**
   * Create a Channel from the protocol string representation of it
   *
   * @param value of the channel
   * @return the new channel
   */
  public static Channel fromString(String value) {
    // Remove the "/root/" part of the value as it is not relevant and does not need to be stored
    String relevantPart = value.substring(ROOT_CHANNEL.length() + 1);
    return new Channel(relevantPart.split(DELIMITER));
  }

  /**
   * Create a new LAO Channel (/root/<lao_id>)
   *
   * @param laoId of the lao
   * @return the new channel
   */
  public static Channel getLaoChannel(String laoId) {
    return Channel.ROOT.subChannel(laoId);
  }

  // ================ Object itself =================

  // This list contains the different parts of the channel except the root as every channel has a
  // root part
  // So for the channel /root/lao_id/election_id, the elements will be [lao_id, election_id]
  @NonNull private final List<String> segments;

  /**
   * Copy constructor
   *
   * @param channel the channel to be deep copied
   */
  public Channel(Channel channel) {
    this.segments = new ArrayList<>(channel.segments);
  }

  private Channel(String... segments) {
    this.segments = Arrays.asList(segments);

    if (!CHANNEL_VALIDATOR.test(getAsString()))
      throw new IllegalArgumentException(getAsString() + " is not a valid channel");
  }

  private Channel(Channel base, String subSegments) {
    List<String> newSegments = new ArrayList<>(base.segments);
    newSegments.add(subSegments);
    this.segments = Collections.unmodifiableList(newSegments);

    if (!CHANNEL_VALIDATOR.test(getAsString()))
      throw new IllegalArgumentException(getAsString() + " is not a valid channel");
  }

  /**
   * Return a channel that is this channel with one more element underneath
   *
   * @param segment to append to this channel
   * @return a new Channel object
   */
  public Channel subChannel(String segment) {
    return new Channel(this, segment);
  }

  /**
   * @return true if this channel is of the form '/root/lao_id'
   */
  public boolean isLaoChannel() {
    return segments.size() == 1;
  }

  /**
   * Check if the channel could be an election channel.
   *
   * <p>This check is not perfect, any channel of the form /root/xxx/xxx are accepted as valid
   * election channel
   *
   * <p>Be careful when using this function
   *
   * @return true if this channel could be an election channel
   */
  public boolean isElectionChannel() {
    return segments.size() == 2;
  }

  /**
   * @return the LAO ID contained in this channel
   */
  public String extractLaoId() {
    if (segments.isEmpty())
      throw new IllegalStateException(
          "This channel is the root channel and does not contain any LAO ID");

    // In the current implementation of the protocol, the first element of the channel (if present)
    // is always the LAO id
    return segments.get(0);
  }

  /**
   * Retrieve the election id contained in this channel
   *
   * <p>Warning, there is no guarantee that the retrieved value is in fact an election id. This will
   * only work if the function is performed on an election channel
   *
   * @return the election id contained in this channel
   */
  public String extractElectionId() {
    if (segments.size() < 2)
      throw new IllegalStateException("This channel is not an election channel");

    return segments.get(1);
  }

  /**
   * @return the protocol like String representation of the channel (/root/abc/efg)
   */
  public String getAsString() {
    return ROOT_CHANNEL + segments.stream().map(e -> "/" + e).collect(Collectors.joining());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Channel channel = (Channel) o;
    return segments.equals(channel.segments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segments);
  }

  @NonNull
  @Override
  public String toString() {
    return "Channel{'" + getAsString() + "'}";
  }
}
