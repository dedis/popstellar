package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpDao
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpEntity
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionDao
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionEntity
import com.github.dedis.popstellar.utility.GeneralUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.error.UnknownChirpException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class is the repository of the social media feature
 *
 * Its main purpose is to store chirps and publish updates
 */
@Singleton
class SocialMediaRepository
@Inject
constructor(appDatabase: AppDatabase, application: Application) {
  private val chirpsByLao: MutableMap<String, LaoChirps> = HashMap()
  private val reactionDao: ReactionDao = appDatabase.reactionDao()
  private val chirpDao: ChirpDao = appDatabase.chirpDao()
  private val disposables = CompositeDisposable()

  init {
    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
  }

  /**
   * Add a new chirp to the repository.
   *
   * If the chirp already exist, it will be overridden
   *
   * @param laoId id of the lao the chirp was sent on
   * @param chirp to add
   */
  fun addChirp(laoId: String, chirp: Chirp) {
    Timber.tag(TAG).d("Adding new chirp on lao %s : %s", laoId, chirp)

    // Persist the chirp
    disposables.add(
        chirpDao
            .insert(ChirpEntity(laoId, chirp))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.tag(TAG).d("Successfully persisted chirp %s", chirp.id) },
                { err: Throwable ->
                  Timber.tag(TAG).e(err, "Error in persisting chirp %s", chirp.id)
                }))

    // Retrieve Lao data and add the chirp to it
    getLaoChirps(laoId).add(chirp)
  }

  /**
   * Delete a chirp based on its id
   *
   * @param id of the chirp to delete
   * @return true if a chirp with given id existed
   */
  fun deleteChirp(laoId: String, id: MessageID): Boolean {
    Timber.tag(TAG).d("Deleting chirp on lao %s with id %s", laoId, id)
    return getLaoChirps(laoId).delete(id)
  }

  /** @return the observable of a specific chirp */
  @Throws(UnknownChirpException::class)
  fun getChirp(laoId: String, id: MessageID): Observable<Chirp> {
    return getLaoChirps(laoId).getChirp(id)
  }

  /** @return the observable of a specific chirp's reactions */
  @Throws(UnknownChirpException::class)
  fun getReactions(laoId: String, chirpId: MessageID): Observable<Set<Reaction>> {
    return getLaoChirps(laoId).getReactions(chirpId)
  }

  @Synchronized
  fun getReactionsByChirp(laoId: String, chirpId: MessageID): Set<Reaction> {
    return getLaoChirps(laoId).reactionByChirpId[chirpId] ?: emptySet()
  }

    fun queryMoreChirps(laoId: String) {
        getLaoChirps(laoId).queryMoreChirps()
    }

  /**
   * @param laoId of the lao we want to observe the chirp list
   * @return an observable set of message ids whose correspond to the set of chirp published on the
   *   given lao
   */
  fun getChirpsOfLao(laoId: String): Observable<Set<MessageID>> {
    return getLaoChirps(laoId).getChirpsSubject()
  }

  /**
   * Add a reaction to a given chirp.
   *
   * @param laoId id of the lao the reaction was sent on
   * @param reaction reaction to add
   * @return true if the chirp associated with the given reaction exists, false otherwise
   */
  fun addReaction(laoId: String, reaction: Reaction): Boolean {
    Timber.tag(TAG).d("Adding new reaction on lao %s : %s", laoId, reaction)

    // Persist the reaction
    disposables.add(
        reactionDao
            .insert(ReactionEntity(reaction))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.tag(TAG).d("Successfully persisted reaction %s", reaction.id) },
                { err: Throwable ->
                  Timber.tag(TAG).e(err, "Error in persisting reaction %s", reaction.id)
                }))

    // Retrieve Lao data and add the reaction to it
    return getLaoChirps(laoId).addReaction(reaction)
  }

  /**
   * Delete a reaction based on its id.
   *
   * @param laoId id of the lao the reaction was sent on
   * @param reactionID identifier of the reaction to delete
   * @return true if the reaction with the given id exists and that reaction refers to an existing
   *   chirp, false otherwise
   */
  fun deleteReaction(laoId: String, reactionID: MessageID): Boolean {
    Timber.tag(TAG).d("Deleting reaction on lao %s : %s", laoId, reactionID)
    // Retrieve Lao data and delete the reaction from it
    return getLaoChirps(laoId).deleteReaction(reactionID)
  }

  @Synchronized
  private fun getLaoChirps(laoId: String): LaoChirps {
    // Create the lao chirps object if it is not present yet
    return chirpsByLao.computeIfAbsent(laoId) { LaoChirps(this, laoId) }
  }

  /**
   * This class holds the social media data of a specific lao
   *
   * Its purpose is to hold data in a way that it is easier to handle and understand. It is also a
   * way to avoid any conflict between laos.
   */
  private class LaoChirps(
      private val repository: SocialMediaRepository,
      private val laoId: String
  ) {
    // Chirps
    private val chirps = ConcurrentHashMap<MessageID, Chirp>()
    private val chirpSubjects = ConcurrentHashMap<MessageID, Subject<Chirp>>()
    private val chirpsSubject: Subject<Set<MessageID>> = BehaviorSubject.createDefault(emptySet())
      //TODO : used to fake page loading, to be removed when we will be able to request only wanted chirps. Maxime Teuber @Kaz-ookid - April 2024
      private var chirpsLoaded : AtomicInteger = AtomicInteger(0)
      private var storageIsLoaded : Boolean = false

    // Reactions
    val reactionByChirpId = ConcurrentHashMap<MessageID, MutableSet<Reaction>>()
    private val reactions = ConcurrentHashMap<MessageID, Reaction>()
    private val reactionSubjectsByChirpId = ConcurrentHashMap<MessageID, Subject<Set<Reaction>>>()

    init {
      loadStorage()
        chirpsLoaded.set(CHIRPS_PAGE_SIZE)
    }

    fun add(chirp: Chirp) {
      val id = chirp.id
      val old = chirps[id]
      if (old != null) {
        Timber.tag(TAG).w("A chirp with id %s already exist : %s", id, old)
        return
      }

      // Update repository data
      chirps[id] = chirp
      reactionByChirpId.putIfAbsent(chirp.id, ConcurrentHashMap.newKeySet())
      reactionSubjectsByChirpId.putIfAbsent(chirp.id, BehaviorSubject.createDefault(HashSet()))

      // Publish new values on subjects
      chirpSubjects[id] = BehaviorSubject.createDefault(chirp)
      chirpsSubject.toSerialized().onNext(HashSet(chirps.keys))

        if (storageIsLoaded) {
            //TODO : used to fake page loading, to be removed when we will be able to request only wanted chirps. Maxime Teuber @Kaz-ookid - April 2024
            // if the storage is loaded, we can increment the number of chirps loaded
            chirpsLoaded.incrementAndGet()
        }
    }

      //TODO : used to fake page loading, to be removed when we will be able to request only wanted chirps. Maxime Teuber @Kaz-ookid - April 2024
      // Either here or directly in DB, we should query to servers tne CHIRPS_PAGE_SIZE next chirps, and not all of them.
      fun queryMoreChirps() {
        chirpsLoaded.addAndGet(CHIRPS_PAGE_SIZE)
          chirpsSubject.onNext(chirps.keys.sortedByDescending { chirps[it]?.timestamp }.take(chirpsLoaded.get()).toSet())
      }

    fun addReaction(reaction: Reaction): Boolean {
      // Check if the associated chirp is present
      val chirp = chirps[reaction.chirpId] ?: return false
      val chirpReactions = reactionByChirpId.getValue(chirp.id)

      // Search for a previous deleted reaction
      val deleted = reactions[reaction.id]
      if (deleted != null) {
        chirpReactions.remove(deleted)
      }

      // Update repository data
      reactions[reaction.id] = reaction
      chirpReactions.add(reaction)
      reactionSubjectsByChirpId[chirp.id]?.toSerialized()?.onNext(HashSet(chirpReactions))
      return true
    }

    fun delete(id: MessageID): Boolean {
      val chirp = chirps[id] ?: return false
      if (chirp.isDeleted) {
        Timber.tag(TAG).d("The chirp with id %s is already deleted", id)
      } else {
        val subject =
            chirpSubjects[id]
                ?: // This should really never occurs
                error("A chirp exist but has no associated subject with it")
        val deleted = chirp.deleted()
        chirps[id] = deleted
        subject.toSerialized().onNext(deleted)

        // Persist the deleted reaction (done only for completeness, this is not necessary)
        repository.disposables.add(
            repository.chirpDao
                .insert(ChirpEntity(laoId, deleted))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { Timber.tag(TAG).d("Successfully persisted deleted chirp %s", deleted.id) },
                    { err: Throwable ->
                      Timber.tag(TAG).e(err, "Error in persisting deleted chirp %s", deleted.id)
                    }))
      }
      return true
    }

    fun deleteReaction(reactionId: MessageID): Boolean {
      // Check if the associated reaction is present
      val reaction = reactions[reactionId] ?: return false
      val chirp = chirps[reaction.chirpId] ?: error("The reaction refers to a not existing chirp")
      // If the chirp the reaction refers to it's not present then throw an error
      if (reaction.isDeleted) {
        Timber.tag(TAG).d("The reaction with id %s is already deleted", reactionId)
      } else {
        // Update the repository data
        val deleted = reaction.deleted()
        reactions[reactionId] = deleted
        val chirpReactions = reactionByChirpId.getValue(chirp.id)

        // Replace the old reaction with the deleted one
        chirpReactions.remove(reaction)
        chirpReactions.add(deleted)
        reactionSubjectsByChirpId[chirp.id]?.toSerialized()?.onNext(chirpReactions)

        // Persist the deleted reaction (done only for completeness, this is not necessary)
        repository.disposables.add(
            repository.reactionDao
                .insert(ReactionEntity(deleted))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { Timber.tag(TAG).d("Successfully persisted deleted reaction %s", deleted.id) },
                    { err: Throwable ->
                      Timber.tag(TAG).e(err, "Error in persisting deleted reaction %s", deleted.id)
                    }))
      }
      return true
    }

    fun getChirpsSubject(): Observable<Set<MessageID>> {
        //TODO : used to fake page loading, to be removed when we will be able to request only wanted chirps. Maxime Teuber @Kaz-ookid - April 2024
        // would normally return chirpsSubject

        // order chirpsSubject by timestamp then take the first chirpsLoaded
        return chirpsSubject.map { chirps.keys.sortedByDescending { chirps[it]?.timestamp }.take(chirpsLoaded.get()).toSet() }
    }

    @Throws(UnknownChirpException::class)
    fun getChirp(id: MessageID): Observable<Chirp> {
      return chirpSubjects[id] ?: throw UnknownChirpException(id)
    }

    @Throws(UnknownChirpException::class)
    fun getReactions(chirpId: MessageID): Observable<Set<Reaction>> {
      return reactionSubjectsByChirpId[chirpId] ?: throw UnknownChirpException(chirpId)
    }

    /**
     * Load in memory the chirps and their respective reactions from the disk only when the user
     * wants to inflate the chirps adapter. It can be done only once per LAO, as during the
     * execution everything is also stored in memory.
     */
    private fun loadStorage() {
      repository.disposables.add(
          repository.chirpDao.getChirpsByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  { chirpsList: List<Chirp>? ->
                    chirpsList?.forEach(
                        Consumer outer@{ chirp: Chirp ->
                          // Do not retrieve deleted chirps
                          if (chirp.isDeleted) {
                            return@outer
                          }
                          // Load the chirp into the memory
                          add(chirp)
                          Timber.tag(TAG).d("Retrieved from db chirp %s", chirp.id)
                          // When retrieving the chirp also retrieve its reactions
                          repository.disposables.add(
                              repository.reactionDao
                                  .getReactionsByChirpId(chirp.id)
                                  .subscribeOn(Schedulers.io())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(
                                      { reactionsList: List<Reaction>? ->
                                        reactionsList?.forEach(
                                            Consumer inner@{ reaction: Reaction ->
                                              // Do not retrieve deleted reactions
                                              if (reaction.isDeleted) {
                                                return@inner
                                              }
                                              // Load the reaction into the memory
                                              addReaction(reaction)
                                              Timber.tag(TAG)
                                                  .d("Retrieved from db reaction %s", reaction.id)
                                            })
                                      },
                                      { err: Throwable ->
                                        Timber.tag(TAG)
                                            .e(
                                                err,
                                                "No reaction found in the storage for chirp %s",
                                                chirp.id)
                                      }))
                        })
                      storageIsLoaded = true
                  } ,
                  { err: Throwable ->
                    Timber.tag(TAG).e(err, "No chirp found in the storage for lao %s", laoId)
                  }))
    }
  }

    companion object {
        private const val CHIRPS_PAGE_SIZE : Int = 10
    private val TAG = SocialMediaRepository::class.java.simpleName
  }
}
