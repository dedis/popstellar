package com.github.dedis.popstellar.model.network.answer;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A succeed query's answer */
public final class Result extends Answer {

  private Optional<Integer> general;
  private Optional<List<MessageGeneral>> messages;

  /**
   * Constructor of a Result
   *
   * @param id of the answer
   */
  public Result(int id) {
    super(id);
    this.general = Optional.empty();
    this.messages = Optional.empty();
  }

  public void setGeneral() {
    this.general = Optional.of(0);
  }

  public void setMessages(List<MessageGeneral> messages) {
    this.messages = Optional.of(messages);
  }

  public Optional<Integer> getGeneral() {
    return general;
  }

  public Optional<List<MessageGeneral>> getMessages() {
    return messages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Result result = (Result) o;
    return Objects.equals(getGeneral(), result.getGeneral())
        && Objects.equals(getMessages(), result.getMessages());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getGeneral(), getMessages());
  }

  @Override
  public String toString() {
    return "Result{" + "general=" + general + ", messages=" + messages + '}';
  }
}
