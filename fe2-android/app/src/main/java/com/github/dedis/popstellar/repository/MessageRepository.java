package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;
import android.util.LruCache;

import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@Singleton
public class MessageRepository {

  private static final String TAG = MessageRepository.class.getSimpleName();

  /** Size of the LRU cache */
  private static final int CACHED_MESSAGES = 100;

  /**
   * Ephemeral messages are all those messages which are not needed to be persisted on the disk, so
   * they're only stored in memory. The messages persisted can be found in the Objects class, see
   * {@link
   * com.github.dedis.popstellar.model.network.method.message.data.Objects#hasToBePersisted())}
   *
   * <p>Other messages on the other hand are persisted, but only with their ids, as this is what is
   * used to avoid reprocessing, as the relative object have already been processed and added to the
   * other repositories. The messages for which we store the actual content can be found here {@link
   * com.github.dedis.popstellar.model.network.method.message.data.Action#isStoreNeededByAction())}
   */
  private final ConcurrentHashMap<MessageID, MessageGeneral> ephemeralMessages =
      new ConcurrentHashMap<>();

  /** Cache for efficient lookups and for avoiding I/O operations */
  private final LruCache<MessageID, MessageGeneral> messageCache = new LruCache<>(CACHED_MESSAGES);

  private final MessageDao messageDao;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public MessageRepository(AppDatabase appDatabase, Application application) {
    messageDao = appDatabase.messageDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
    // Full the cache at starting time
    loadCache();
  }

  /** This function is called at creation to fill the cache asynchronously */
  private void loadCache() {
    disposables.add(
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
                                msg.getContent() == null
                                    ? MessageGeneral.emptyMessage()
                                    : msg.getContent())),
                err -> Timber.tag(TAG).e(err, "Error loading message repository cache")));
  }

  /**
   * This function gets a message from the repository given its unique identifier.
   *
   * @param messageID identifier of the message to retrieve
   * @return the message if present, null if absent
   */
  public MessageGeneral getMessage(MessageID messageID) {
    // Check if it's an ephemeral message, so no need to look up in the db
    MessageGeneral ephemeralMessage = ephemeralMessages.get(messageID);
    if (ephemeralMessage != null) {
      return ephemeralMessage;
    }

    // Retrieve from cache if present
    synchronized (messageCache) {
      MessageGeneral cachedMessage = messageCache.get(messageID);
      if (cachedMessage != null) {
        return cachedMessage;
      }
    }

    // Search in the db
    MessageEntity messageEntity = messageDao.getMessageById(messageID);
    if (messageEntity != null) {
      MessageGeneral messageGeneral = messageEntity.getContent();
      // Put it into cache
      synchronized (messageCache) {
        messageCache.put(messageID, messageGeneral);
      }
      return messageGeneral;
    }

    return null;
  }

  /**
   * This function adds a message to the repository.
   *
   * @param message message to add to the repository
   * @param isContentNeeded boolean that specifies whether the content of the message is useful to
   *     be stored (true) or if it's not needed and memory could be saved (false)
   * @param toPersist boolean that specifies whether the message has to be persisted and saved in
   *     the db (true) or only loaded in memory (false)
   */
  public void addMessage(MessageGeneral message, boolean isContentNeeded, boolean toPersist) {
    MessageID messageID = message.getMessageId();
    if (!isContentNeeded) {
      // No need to store the content of the message, just the id is needed
      // However the cache and the concurrent hash map don't accept null value
      // Thus we use an empty messages that is light-weight (whose ref is also shared)
      message = MessageGeneral.emptyMessage();
    }

    if (!toPersist) {
      ephemeralMessages.put(messageID, message);
    } else {
      // Add the message to the cache (cache cannot accept a null value)
      synchronized (messageCache) {
        messageCache.put(messageID, message);
      }

      // Add asynchronously the messages to the database
      disposables.add(
          messageDao
              .insert(new MessageEntity(messageID, message.isEmpty() ? null : message))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  () -> Timber.tag(TAG).d("Persisted message %s", messageID),
                  err -> Timber.tag(TAG).e(err, "Error persisting the message %s", messageID)));
    }
  }

  /**
   * This function searches if a given message is stored in the repository. This is used to avoid
   * message reprocessing.
   *
   * @param messageID identifier of the message
   * @param isPersisted boolean that specifies whether the message should be searched only in the
   *     memory (if false), or also in the disk (if true)
   * @return true if the message is present, false otherwise
   */
  public boolean isMessagePresent(MessageID messageID, boolean isPersisted) {
    // Avoid I/O operation if it's an ephemeral message
    if (!isPersisted) {
      return ephemeralMessages.containsKey(messageID);
    }

    // Check if it's already in cache
    synchronized (messageCache) {
      MessageGeneral messageGeneral = messageCache.get(messageID);
      if (messageGeneral != null) {
        return true;
      }
    }

    // Otherwise perform an I/O operation
    return messageDao.getMessageById(messageID) != null;
  }
}
