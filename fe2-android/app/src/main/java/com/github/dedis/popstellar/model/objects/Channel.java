package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class represent a channel used to subscribe to a certain part of the messages. More detailed
 * information are given in the protocol specification of the application.
 *
 * <p>It is implemented in a functional manner to avoid any unintended modification
 */
public class Channel {

  private static final String ROOT_CHANNEL = "/root";
  private static final String DELIMITER = "/";
  // Predicate matching a String with the protocol regex of a channel
  private static final Predicate<String> CHANNEL_VALIDATOR =
      Pattern.compile("^/root(/[^/]+)*$").asPredicate();

  /** Root channel, every client is subscribed to it. */
  public static final Channel ROOT = new Channel();

  public static Channel newChannel(String value) {
    // Avoid creating duplicate instances
    if (ROOT_CHANNEL.equals(value)) return ROOT;

    // Remove the "/root/" part of the value as it is not relevant and does not need to be stored
    String relevantPart = value.substring(ROOT_CHANNEL.length() + 1);
    return new Channel(relevantPart.split(DELIMITER));
  }

  // ================ Object itself =================

  // This list contains the different parts of the channel except the root as every channel has a
  // root part
  // So for the channel /root/lao_id/election_id, the elements will be [lao_id, election_id]
  @NonNull private final List<String> elements;

  private Channel(String... elements) {
    this.elements = Arrays.asList(elements);

    if (!CHANNEL_VALIDATOR.test(getAsString()))
      throw new IllegalArgumentException(getAsString() + " is not a valid channel");
  }

  private Channel(Channel base, String subElement) {
    List<String> elements = new ArrayList<>(base.elements);
    elements.add(subElement);
    this.elements = Collections.unmodifiableList(elements);

    if (!CHANNEL_VALIDATOR.test(getAsString()))
      throw new IllegalArgumentException(getAsString() + " is not a valid channel");
  }

  /**
   * Return a channel that is this channel with one more element underneath
   *
   * @param subElement element to append to this channel
   * @return a new Channel object
   */
  public Channel sub(String subElement) {
    return new Channel(this, subElement);
  }

  /** @return true if this channel is of the form '/root/lao_id' */
  public boolean isLaoChannel() {
    return elements.size() == 1;
  }

  /** @return true if this channel could be an election channel */
  public boolean isElectionChannel() {
    return elements.size() == 2;
  }

  /** @return the LAO ID contained in this channel */
  public String extractLaoId() {
    if (elements.isEmpty())
      throw new IllegalStateException(
          "This channel is the root channel and does not contain any LAO ID");

    // In the current implementation of the protocol, the first element of the channel (if present)
    // is always the LAO id
    return elements.get(0);
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
    if (elements.size() < 2)
      throw new IllegalStateException("This channel is not an election channel");

    return elements.get(1);
  }

  /** @return the protocol like String representation of the channel (/root/abc/efg) */
  public String getAsString() {
    return ROOT_CHANNEL + elements.stream().map(e -> "/" + e).collect(Collectors.joining());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Channel channel = (Channel) o;
    return elements.equals(channel.elements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(elements);
  }

  @NonNull
  @Override
  public String toString() {
    return "Channel{'" + getAsString() + "'}";
  }
}
