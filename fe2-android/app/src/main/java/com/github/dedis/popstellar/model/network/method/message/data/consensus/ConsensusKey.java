package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import java.util.Objects;

public final class ConsensusKey {

  private final String type;
  private final String id;
  private final String property;

  public ConsensusKey(String type, String id, String property) {
    this.type = type;
    this.id = id;
    this.property = property;
  }

  public String getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public String getProperty() {
    return property;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, id, property);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusKey that = (ConsensusKey) o;
    return java.util.Objects.equals(getType(), that.getType())
        && java.util.Objects.equals(getId(), that.getType())
        && java.util.Objects.equals(getProperty(), that.getProperty());
  }

  @Override
  public String toString() {
    return String.format("ConsensusKey{id='%s', type='%s', property='%s'}", id, type, property);
  }
}
