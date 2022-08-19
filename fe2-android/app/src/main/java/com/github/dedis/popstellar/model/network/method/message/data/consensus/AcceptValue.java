package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Immutable
public final class AcceptValue {

  @SerializedName("accepted_try")
  private final int acceptedTry;

  @SerializedName("accepted_value")
  private final boolean acceptedValue;

  public AcceptValue(int acceptedTry, boolean acceptedValue) {
    this.acceptedTry = acceptedTry;
    this.acceptedValue = acceptedValue;
  }

  public int getAcceptedTry() {
    return acceptedTry;
  }

  public boolean isAcceptedValue() {
    return acceptedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AcceptValue that = (AcceptValue) o;

    return acceptedTry == that.acceptedTry && acceptedValue == that.acceptedValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptedTry, acceptedValue);
  }

  @Override
  public String toString() {
    return String.format(
        "AcceptValue{accepted_try=%s, accepted_value=%b}", acceptedTry, acceptedValue);
  }
}
