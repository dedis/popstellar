package com.github.dedis.popstellar.model.network.answer;

import com.github.dedis.popstellar.model.network.GenericMessage;

import java.util.Objects;

/**
 * An abstract result from a request
 *
 * <p>Is linked to an earlier request with a unique id
 */
public abstract class Answer extends GenericMessage {

  private final int id;

  /**
   * Constructor of an Answer
   *
   * @param id of the answer
   */
  public Answer(int id) {
    this.id = id;
  }

  /** Returns the ID of the answer */
  public int getId() {
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
    Answer answer = (Answer) o;
    return getId() == answer.getId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
