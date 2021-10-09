package com.github.dedis.popstellar.model.network.answer;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import java.util.List;

/** A succeed query's answer with a list of MessageGeneral */
public class ResultMessages extends Result {

  private final List<MessageGeneral> messages;
  /**
   * Constructor of a ResultMessages
   *
   * @param id of the answer
   * @param messages of the answer
   */
  public ResultMessages(int id, List<MessageGeneral> messages) {
    super(id);
    this.messages = messages;
  }

  public List<MessageGeneral> getMessages() {
    return messages;
  }
}
