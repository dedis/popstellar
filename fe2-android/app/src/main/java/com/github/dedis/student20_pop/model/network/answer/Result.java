package com.github.dedis.student20_pop.model.network.answer;

import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import java.util.List;
import java.util.Optional;

/** A succeed query's answer */
public final class Result extends Answer {

  private Optional<Integer> general;
  private Optional<List<MessageGeneral>> messages;

  /**
   * Constructor of a Result
   *
   * @param id of the answer
   * @param result of the answer
   */
  public Result(int id) {
    super(id);
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
}
