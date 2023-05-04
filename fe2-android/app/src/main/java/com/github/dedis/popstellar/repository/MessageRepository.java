package com.github.dedis.popstellar.repository;

import android.annotation.SuppressLint;
import android.util.LruCache;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@Singleton
public class MessageRepository {

  private static final String TAG = MessageRepository.class.getSimpleName();

  /** Size of the LRU cache */
  private static final int CACHED_MESSAGES = 150;

  /**
   * Ephemeral messages are all those messages which are not needed to be persisted. So far only the
   * laos messages are persistent, but they will be extended in the future
   */
  private final Map<MessageID, MessageGeneral> ephemeralMessages = new HashMap<>();

  private final LruCache<MessageID, MessageGeneral> messageCache = new LruCache<>(CACHED_MESSAGES);
  private final MessageDao messageDao;

  @Inject
  public MessageRepository(AppDatabase appDatabase) {
    this.messageDao = appDatabase.messageDao();
    loadCache();
  }

  /** This function is called on creation to asynchronously full the cache */
  @SuppressLint("CheckResult")
  private void loadCache() {
    messageDao
        .takeFirstNMessages(CACHED_MESSAGES)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            messageEntities ->
                messageEntities.forEach(
                    msg ->
                        messageCache.put(
                            msg.getMessageId(),
                            // Cache doesn't accept null as value, so an empty message is used
                            // instead
                            msg.getContent() == null
                                ? MessageGeneral.emptyMessage()
                                : msg.getContent())),
            Exceptions::propagate);
  }

  public MessageGeneral getMessage(MessageID messageID) {
    // Check if it's an ephemeral message, so no need to look up in the db
    MessageGeneral ephemeralMessage = ephemeralMessages.get(messageID);
    if (ephemeralMessage != null) {
      return ephemeralMessage;
    }

    // Retrieve from cache if present
    MessageGeneral cachedMessage = messageCache.get(messageID);
    if (cachedMessage != null) {
      return cachedMessage;
    }

    // Search in the db
    MessageEntity messageEntity = messageDao.getMessageById(messageID);
    if (messageEntity != null) {
      MessageGeneral messageGeneral = messageEntity.getContent();
      // Put it into cache
      messageCache.put(messageID, messageGeneral);
      return messageGeneral;
    }

    return null;
  }

  /**
   * This function adds a message to the repository.
   *
   * @param message message to add to the repository
   * @param isStoringNeeded boolean that specifies whether the content of the message is useful to
   *     be stored (true) or if it's not needed and memory could be saved (false)
   * @param toPersist boolean that specifies whether the message has to be persisted and saved in
   *     the db (true) or only loaded in memory (false)
   */
  public void addMessage(MessageGeneral message, boolean isStoringNeeded, boolean toPersist) {
    MessageID messageID = message.getMessageId();
    if (!isStoringNeeded) {
      // No need to store the content of the message, just the id is needed
      message = null;
    }

    if (!toPersist) {
      ephemeralMessages.put(messageID, message);
    } else {
      // Add the message to the cache (cache cannot accept a null value)
      messageCache.put(messageID, message == null ? MessageGeneral.emptyMessage() : message);

      // Add asynchronously the messages to the database
      messageDao
          .insert(new MessageEntity(messageID, message))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnComplete(() -> Timber.tag(TAG).d("Persisted message %s", messageID))
          .subscribe();
    }
  }

  public boolean isMessagePresent(MessageID messageID, boolean isPersisted) {
    // Avoid I/O operation if it's an ephemeral message
    if (!isPersisted) {
      return ephemeralMessages.containsKey(messageID);
    }

    // Check if it's already in cache
    MessageGeneral messageGeneral = messageCache.get(messageID);
    if (messageGeneral != null) {
      return true;
    }

    // Otherwise perform an I/O operation
    return messageDao.getMessageById(messageID) != null;
  }
}
