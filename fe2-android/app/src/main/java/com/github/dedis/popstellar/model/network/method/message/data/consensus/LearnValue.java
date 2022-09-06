package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;

import java.util.Objects;

@Immutable
public final class LearnValue {

  private final boolean decision;

  public LearnValue(boolean decision) {
    this.decision = decision;
  }

  public boolean isDecision() {
    return decision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LearnValue that = (LearnValue) o;

    return decision == that.decision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(decision);
  }

  @Override
  public String toString() {
    return String.format("LearnValue{decision=%b}", decision);
  }
}
