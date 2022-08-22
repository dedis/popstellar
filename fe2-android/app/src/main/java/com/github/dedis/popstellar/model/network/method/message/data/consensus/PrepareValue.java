package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Immutable
public final class PrepareValue {

  @SerializedName("proposed_try")
  private final int proposedTry;

  public PrepareValue(int proposedTry) {
    this.proposedTry = proposedTry;
  }

  public int getProposedTry() {
    return proposedTry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrepareValue that = (PrepareValue) o;

    return proposedTry == that.proposedTry;
  }

  @Override
  public int hashCode() {
    return Objects.hash(proposedTry);
  }

  @Override
  public String toString() {
    return String.format("PrepareValue{proposed_try=%s}", proposedTry);
  }
}
