package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import android.util.LruCache
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.database.message.MessageEntity
import com.github.dedis.popstellar.utility.GeneralUtils.buildLifecycleCallback
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class MessageRepository @Inject constructor(appDatabase: AppDatabase, application: Application) {
  /**
   * Ephemeral messages are all those messages which are not needed to be persisted on the disk, so
   * they're only stored in memory. The messages persisted can be found in the Objects class, see
   * [ ][com.github.dedis.popstellar.model.network.method.message.data.Objects.hasToBePersisted]
   *
   * Other messages on the other hand are persisted, but only with their ids, as this is what is
   * used to avoid reprocessing, as the relative object have already been processed and added to the
   * other repositories. The messages for which we store the actual content can be found here
   * [ ][com.github.dedis.popstellar.model.network.method.message.data.Action.isStoreNeededByAction]
   */
  private val ephemeralMessages = ConcurrentHashMap<MessageID, MessageGeneral>()

  /** Cache for efficient lookups and for avoiding I/O operations */
  private val messageCache = LruCache<MessageID, MessageGeneral>(CACHED_MESSAGES)
  private val messageDao: MessageDao
  private val disposables = CompositeDisposable()

  init {
    messageDao = appDatabase.messageDao()

    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
    // Full the cache at starting time
    loadCache()
  }

  /** This function is called at creation to fill the cache asynchronously */
  private fun loadCache() {
    disposables.add(
        messageDao
            .takeFirstNMessages(CACHED_MESSAGES)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { messageEntities: List<MessageEntity>? ->
                  messageEntities?.forEach(
                      Consumer { msg: MessageEntity ->
                        messageCache.put(
                            msg.messageId,
                            // Cache doesn't accept null as value, so an empty message is used
                            msg.content ?: MessageGeneral.emptyMessage())
                      })
                },
                { err: Throwable ->
                  Timber.tag(TAG).e(err, "Error loading message repository cache")
                }))
  }

  /**
   * This function gets a message from the repository given its unique identifier.
   *
   * @param messageID identifier of the message to retrieve
   * @return the message if present, null if absent
   */
  fun getMessage(messageID: MessageID): MessageGeneral? {
    // Check if it's an ephemeral message, so no need to look up in the db
    val ephemeralMessage = ephemeralMessages[messageID]
    if (ephemeralMessage != null) {
      return ephemeralMessage
    }

    // Retrieve from cache if present
    synchronized(messageCache) {
      val cachedMessage = messageCache[messageID]
      if (cachedMessage != null) {
        return cachedMessage
      }
    }

    // Search in the db
    val messageEntity = messageDao.getMessageById(messageID)
    if (messageEntity != null) {
      val messageGeneral = messageEntity.content
      // Put it into cache
      synchronized(messageCache) { messageCache.put(messageID, messageGeneral) }
      return messageGeneral
    }
    return null
  }

  /**
   * This function adds a message to the repository.
   *
   * @param message message to add to the repository
   * @param isContentNeeded boolean that specifies whether the content of the message is useful to
   *   be stored (true) or if it's not needed and memory could be saved (false)
   * @param toPersist boolean that specifies whether the message has to be persisted and saved in
   *   the db (true) or only loaded in memory (false)
   */
  fun addMessage(message: MessageGeneral, isContentNeeded: Boolean, toPersist: Boolean) {
    var msg = message
    if (!isContentNeeded) {
      // No need to store the content of the message, just the id is needed
      // However the cache and the concurrent hash map don't accept null value
      // Thus we use an empty messages that is light-weight (whose ref is also shared)
      msg = MessageGeneral.emptyMessage()
    }

    val messageID = message.messageId
    if (!toPersist) {
      ephemeralMessages[messageID] = msg
    } else {
      // Add the message to the cache (cache cannot accept a null value)
      synchronized(messageCache) { messageCache.put(messageID, msg) }

      // Add asynchronously the messages to the database
      disposables.add(
          messageDao
              .insert(MessageEntity(messageID, if (msg.isEmpty) null else msg))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  { Timber.tag(TAG).d("Persisted message %s", messageID) },
                  { err: Throwable ->
                    Timber.tag(TAG).e(err, "Error persisting the message %s", messageID)
                  }))
    }
  }

  /**
   * This function searches if a given message is stored in the repository. This is used to avoid
   * message reprocessing.
   *
   * @param messageID identifier of the message
   * @param isPersisted boolean that specifies whether the message should be searched only in the
   *   memory (if false), or also in the disk (if true)
   * @return true if the message is present, false otherwise
   */
  fun isMessagePresent(messageID: MessageID, isPersisted: Boolean): Boolean {
    // Avoid I/O operation if it's an ephemeral message
    if (!isPersisted) {
      return ephemeralMessages.containsKey(messageID)
    }

    // Check if it's already in cache
    synchronized(messageCache) {
      val messageGeneral = messageCache[messageID]
      if (messageGeneral != null) {
        return true
      }
    }

    // Otherwise perform an I/O operation
    return messageDao.getMessageById(messageID) != null
  }

  companion object {
    private val TAG = MessageRepository::class.java.simpleName

    /** Size of the LRU cache */
    private const val CACHED_MESSAGES = 100
  }
}
