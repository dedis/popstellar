package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageRepository {

  private final Map<MessageID, MessageGeneral> messageById = new HashMap<>();

  @Inject
  public MessageRepository() {
    // Constructor required by Hilt

  }

  public Map<MessageID, MessageGeneral> getMessageById() {
    return messageById;
  }

  public void addMessage(MessageGeneral message) {
    messageById.put(message.getMessageId(), message);
  }
}
