package com.github.dedis.popstellar.repository;

import android.util.LruCache;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageRepository {

  private static final int CACHED_MESSAGES = 100;

  private final LruCache<MessageID, MessageGeneral> messageCache =
      new LruCache<>(CACHED_MESSAGES); // cache up to CACHED_MESSAGES messages

  private final MessageDao messageDao;

  @Inject
  public MessageRepository(AppDatabase appDatabase) {
    messageDao = appDatabase.messageDao();
  }

  public MessageGeneral getMessage(MessageID messageID) {
    // Retrieve from cache if present
    MessageGeneral cachedMessage = messageCache.get(messageID);
    if (cachedMessage != null) {
      return cachedMessage;
    }

    MessageEntity messageEntity = messageDao.getMessageById(messageID);

    // If the message is present in the database
    if (messageEntity != null) {
      MessageGeneral messageGeneral = messageEntity.getContent();
      // Put it into cache
      messageCache.put(messageID, messageGeneral);
      return messageGeneral;
    }

    return null;
  }

  public void addMessage(MessageGeneral message) {
    // Add the message to the cache and the database asynchronously
    messageCache.put(message.getMessageId(), message);
    // Add to the database only the messages we want to persist
    messageDao.insert(new MessageEntity(message.getMessageId(), message));
  }

  public boolean isMessagePresent(MessageID messageID) {
    // Check if it's already in cache
    if (messageCache.get(messageID) != null) {
      return true;
    }

    // Otherwise perform an I/O async operation
    return messageDao.getMessageById(messageID) != null;
  }
}
