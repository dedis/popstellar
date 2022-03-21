package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.objects.Channel;

import java.util.Objects;

/** An abstract low level message that is sent over a specific channel */
public abstract class Message extends GenericMessage {

  private final Channel channel;

  /**
   * Constructor for a Message
   *
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  protected Message(Channel channel) {
    if (channel == null) {
      throw new IllegalArgumentException("Trying to create a message with a null channel");
    }
    this.channel = channel;
  }

  /** Return the Message method */
  public abstract String getMethod();

  /** Returns the message channel */
  public Channel getChannel() {
    return channel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Message that = (Message) o;
    return Objects.equals(channel, that.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel);
  }
}
