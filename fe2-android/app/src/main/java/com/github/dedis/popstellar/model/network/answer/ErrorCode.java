package com.github.dedis.popstellar.model.network.answer;

import java.util.Objects;

/**
 * Error of a failed request
 */
public final class ErrorCode {

  private final int code;
  private final String description;

  /**
   * Constructor of an ErrorCode
   *
   * @param code        the code of the error, as an integer
   * @param description the description of the error
   */
  public ErrorCode(int code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Returns the code of the error.
   */
  public int getCode() {
    return code;
  }

  /**
   * Returns the description of the error.
   */
  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorCode that = (ErrorCode) o;
    return getCode() == that.getCode() && Objects.equals(getDescription(), that.getDescription());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCode(), getDescription());
  }

  @Override
  public String toString() {
    return "ErrorCode{code=" + code + ", description='" + description + "'}";
  }
}
