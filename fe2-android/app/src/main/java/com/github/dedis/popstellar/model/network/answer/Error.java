package com.github.dedis.popstellar.model.network.answer;

import java.util.Objects;

/** A failed query's answer */
public final class Error extends Answer {

  private final ErrorCode error;

  /**
   * Constructor of an Error
   *
   * @param id of the answer
   * @param error of the answer, contains its code and description
   */
  public Error(int id, ErrorCode error) {
    super(id);
    this.error = error;
  }

  /** Returns the error code. */
  public ErrorCode getError() {
    return error;
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
    Error error = (Error) o;
    return Objects.equals(getError(), error.getError());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getError());
  }
}
