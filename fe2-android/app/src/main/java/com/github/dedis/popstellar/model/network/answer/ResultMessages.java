package com.github.dedis.popstellar.model.network.answer;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;

import java.util.*;

/** A succeed query's answer with a list of MessageGeneral */
public final class ResultMessages extends Result {

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultMessages that = (ResultMessages) o;
    return getId() == that.getId() && messages.equals(that.getMessages());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), messages);
  }

  @Override
  public String toString() {
    return "ResultMessages{" + "messages=" + Arrays.toString(messages.toArray()) + '}';
  }
}
