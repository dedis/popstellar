package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.Immutable;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@Immutable
public final class PromiseValue {

  @SerializedName("accepted_try")
  private final int acceptedTry;

  @SerializedName("accepted_value")
  private final boolean acceptedValue;

  @SerializedName("promised_try")
  private final int promisedTry;

  public PromiseValue(int acceptedTry, boolean acceptedValue, int promisedTry) {
    this.acceptedTry = acceptedTry;
    this.acceptedValue = acceptedValue;
    this.promisedTry = promisedTry;
  }

  public int getAcceptedTry() {
    return acceptedTry;
  }

  public boolean isAcceptedValue() {
    return acceptedValue;
  }

  public int getPromisedTry() {
    return promisedTry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PromiseValue that = (PromiseValue) o;

    return acceptedTry == that.acceptedTry
        && acceptedValue == that.acceptedValue
        && promisedTry == that.promisedTry;
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptedTry, acceptedValue, promisedTry);
  }

  @Override
  public String toString() {
    return String.format(
        "PromiseValue{accepted_try=%s, accepted_value=%b, promised_try=%s}",
        acceptedTry, acceptedValue, promisedTry);
  }
}
