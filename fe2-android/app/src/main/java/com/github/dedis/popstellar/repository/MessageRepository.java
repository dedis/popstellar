package com.github.dedis.popstellar.repository;

import android.util.LruCache;

import androidx.room.EmptyResultSetException;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@Singleton
public class MessageRepository {

  private static final int CACHED_MESSAGES = 100;
  private static final String TAG = MessageRepository.class.getSimpleName();

  private final Map<MessageID, MessageGeneral> ephemeralMessages = new HashMap<>();
  private final LruCache<MessageID, MessageGeneral> messageCache = new LruCache<>(CACHED_MESSAGES);

  private final MessageDao messageDao;

  @Inject
  public MessageRepository(AppDatabase appDatabase) {
    messageDao = appDatabase.messageDao();
  }

  public Single<MessageGeneral> getMessage(MessageID messageID) {
    // Check if it's an ephemeral message
    MessageGeneral ephemeralMessage = ephemeralMessages.get(messageID);
    if (ephemeralMessage != null) {
      return Single.just(ephemeralMessage);
    }

    // Retrieve from cache if present
    MessageGeneral cachedMessage = messageCache.get(messageID);
    if (cachedMessage != null) {
      return Single.just(cachedMessage);
    }

    return messageDao
        .getMessageById(messageID)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorResumeNext(
            err -> {
              if (err instanceof EmptyResultSetException) {
                return Single.just(MessageEntity.getEmptyEntity());
              } else {
                return Single.error(err);
              }
            })
        .map(
            entity -> {
              if (entity.equals(MessageEntity.getEmptyEntity())) {
                return null;
              }
              MessageGeneral messageGeneral = entity.getContent();
              // Put it into cache
              messageCache.put(messageID, messageGeneral);
              return messageGeneral;
            });
  }

  public void addMessage(MessageGeneral message, boolean isStoringNeeded, boolean toPersist) {
    MessageID messageID = message.getMessageId();
    if (!isStoringNeeded) {
      // No need to store the content in this case
      message = MessageGeneral.emptyMessage();
    }

    if (!toPersist) {
      ephemeralMessages.put(messageID, message);
    } else {
      // Add the message to the cache
      messageCache.put(messageID, message);
      // Add asynchronously the messages to the database
      messageDao
          .insert(new MessageEntity(messageID, message))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnComplete(() -> Timber.tag(TAG).d("Persisted message %s", messageID))
          .subscribe();
    }
  }

  public Single<Boolean> isMessagePresent(MessageID messageID, boolean isPersisted) {
    // Avoid I/O operation if it's an ephemeral message
    if (!isPersisted) {
      return Single.just(ephemeralMessages.containsKey(messageID));
    }

    // Check if it's already in cache
    MessageGeneral messageGeneral = messageCache.get(messageID);
    if (messageGeneral != null) {
      return Single.just(true);
    }

    // Otherwise perform an I/O operation
    return messageDao
        .getMessageById(messageID)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorResumeNext(
            err -> {
              if (err instanceof EmptyResultSetException) {
                return Single.just(MessageEntity.getEmptyEntity());
              } else {
                return Single.error(err);
              }
            })
        .map(entity -> !entity.equals(MessageEntity.getEmptyEntity()));
  }
}
