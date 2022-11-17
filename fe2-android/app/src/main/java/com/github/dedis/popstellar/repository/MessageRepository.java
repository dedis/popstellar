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

  public MessageGeneral getMessage(MessageID messageID) {
    return messageById.get(messageID);
  }

  public void addMessage(MessageGeneral message) {
    messageById.put(message.getMessageId(), message);
  }

  public boolean isMessagePresent(MessageID messageID) {
    return messageById.containsKey(messageID);
  }
}
