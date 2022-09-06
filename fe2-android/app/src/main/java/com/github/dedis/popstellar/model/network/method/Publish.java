package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Channel;

import java.util.Objects;

/** Publish a message on a channel */
@Immutable
public final class Publish extends Query {

  private final MessageGeneral message;

  /**
   * Constructor for a Publish
   *
   * @param channel name of the channel
   * @param id request ID
   * @param message message to publish
   * @throws IllegalArgumentException if any parameter is null
   */
  public Publish(Channel channel, int id, MessageGeneral message) {
    super(channel, id);
    if (message == null) {
      throw new IllegalArgumentException("Trying to publish a null message");
    }
    this.message = message;
  }

  /** Returns the message to publish. */
  public MessageGeneral getMessage() {
    return message;
  }

  @Override
  public String getMethod() {
    return Method.PUBLISH.getMethod();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Publish publish = (Publish) o;
    return Objects.equals(getMessage(), publish.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMessage());
  }

  @Override
  public String toString() {
    return "Publish{"
        + "id="
        + getRequestId()
        + ", channel='"
        + getChannel()
        + '\''
        + ", message="
        + message
        + '}';
  }
}
