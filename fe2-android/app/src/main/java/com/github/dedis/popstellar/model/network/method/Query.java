package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;

import java.util.Objects;

/**
 * A message that expect an answer later. Therefore, it has a unique id that will be linked with the
 * received answer
 */
public abstract class Query extends Message {

  private final int id;

  /**
   * Constructor for a Query
   *
   * @param channel name of the channel
   * @param id request ID of the query
   */
  protected Query(Channel channel, int id) {
    super(channel);
    this.id = id;
  }

  /** Returns the request ID. */
  public int getRequestId() {
    return id;
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
    Query query = (Query) o;
    return id == query.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), id);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + '{'
        + "id="
        + id
        + ", channel='"
        + getChannel()
        + '\''
        + ", method='"
        + getMethod()
        + '\''
        + '}';
  }
}
