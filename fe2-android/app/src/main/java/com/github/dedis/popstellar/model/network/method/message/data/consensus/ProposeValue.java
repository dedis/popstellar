package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Immutable
public final class ProposeValue {

  @SerializedName("proposed_try")
  private final int proposedTry;

  @SerializedName("proposed_value")
  private final boolean proposedValue;

  public ProposeValue(int proposedTry, boolean proposedValue) {
    this.proposedTry = proposedTry;
    this.proposedValue = proposedValue;
  }

  public int getProposedTry() {
    return proposedTry;
  }

  public boolean isProposedValue() {
    return proposedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProposeValue that = (ProposeValue) o;

    return proposedTry == that.proposedTry && proposedValue == that.proposedValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(proposedTry, proposedValue);
  }

  @Override
  public String toString() {
    return String.format(
        "ProposeValue{proposed_try=%s, proposed_value=%b}", proposedTry, proposedValue);
  }
}
