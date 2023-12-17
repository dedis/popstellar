package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.socialmedia.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * This class is the repository of the social media feature
 *
 * <p>Its main purpose is to store chirps and publish updates
 */
@Singleton
public class SocialMediaRepository {

  private static final String TAG = SocialMediaRepository.class.getSimpleName();

  private final Map<String, LaoChirps> chirpsByLao = new HashMap<>();

  private final ReactionDao reactionDao;
  private final ChirpDao chirpDao;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public SocialMediaRepository(AppDatabase appDatabase, Application application) {
    reactionDao = appDatabase.reactionDao();
    chirpDao = appDatabase.chirpDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  /**
   * Add a new chirp to the repository.
   *
   * <p>If the chirp already exist, it will be overridden
   *
   * @param laoId id of the lao the chirp was sent on
   * @param chirp to add
   */
  public void addChirp(String laoId, Chirp chirp) {
    Timber.tag(TAG).d("Adding new chirp on lao %s : %s", laoId, chirp);

    // Persist the chirp
    disposables.add(
        chirpDao
            .insert(new ChirpEntity(laoId, chirp))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> Timber.tag(TAG).d("Successfully persisted chirp %s", chirp.getId()),
                err -> Timber.tag(TAG).e(err, "Error in persisting chirp %s", chirp.getId())));

    // Retrieve Lao data and add the chirp to it
    getLaoChirps(laoId).add(chirp);
  }

  /**
   * Delete a chirp based on its id
   *
   * @param id of the chirp to delete
   * @return true if a chirp with given id existed
   */
  public boolean deleteChirp(String laoId, MessageID id) {
    Timber.tag(TAG).d("Deleting chirp on lao %s with id %s", laoId, id);
    return getLaoChirps(laoId).delete(id);
  }

  /**
   * @return the observable of a specific chirp
   */
  @NonNull
  public Observable<Chirp> getChirp(String laoId, MessageID id) throws UnknownChirpException {
    return getLaoChirps(laoId).getChirp(id);
  }

  /**
   * @return the observable of a specific chirp's reactions
   */
  @NonNull
  public Observable<Set<Reaction>> getReactions(String laoId, MessageID chirpId)
      throws UnknownChirpException {
    return getLaoChirps(laoId).getReactions(chirpId);
  }

  public synchronized Set<Reaction> getReactionsByChirp(String laoId, MessageID chirpId) {
    return getLaoChirps(laoId).reactionByChirpId.get(chirpId);
  }

  /**
   * @param laoId of the lao we want to observe the chirp list
   * @return an observable set of message ids whose correspond to the set of chirp published on the
   *     given lao
   */
  @NonNull
  public Observable<Set<MessageID>> getChirpsOfLao(String laoId) {
    return getLaoChirps(laoId).getChirpsSubject();
  }

  /**
   * Add a reaction to a given chirp.
   *
   * @param laoId id of the lao the reaction was sent on
   * @param reaction reaction to add
   * @return true if the chirp associated with the given reaction exists, false otherwise
   */
  public boolean addReaction(String laoId, Reaction reaction) {
    Timber.tag(TAG).d("Adding new reaction on lao %s : %s", laoId, reaction);

    // Persist the reaction
    disposables.add(
        reactionDao
            .insert(new ReactionEntity(reaction))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> Timber.tag(TAG).d("Successfully persisted reaction %s", reaction.getId()),
                err ->
                    Timber.tag(TAG).e(err, "Error in persisting reaction %s", reaction.getId())));

    // Retrieve Lao data and add the reaction to it
    return getLaoChirps(laoId).addReaction(reaction);
  }

  /**
   * Delete a reaction based on its id.
   *
   * @param laoId id of the lao the reaction was sent on
   * @param reactionID identifier of the reaction to delete
   * @return true if the reaction with the given id exists and that reaction refers to an existing
   *     chirp, false otherwise
   */
  public boolean deleteReaction(String laoId, MessageID reactionID) {
    Timber.tag(TAG).d("Deleting reaction on lao %s : %s", laoId, reactionID);
    // Retrieve Lao data and delete the reaction from it
    return getLaoChirps(laoId).deleteReaction(reactionID);
  }

  @NonNull
  private synchronized LaoChirps getLaoChirps(String laoId) {
    // Create the lao chirps object if it is not present yet
    return chirpsByLao.computeIfAbsent(laoId, lao -> new LaoChirps(this, laoId));
  }

  /**
   * This class holds the social media data of a specific lao
   *
   * <p>Its purpose is to hold data in a way that it is easier to handle and understand. It is also
   * a way to avoid any conflict between laos.
   */
  private static final class LaoChirps {

    private final SocialMediaRepository repository;
    private final String laoId;

    // Chirps
    private final ConcurrentHashMap<MessageID, Chirp> chirps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MessageID, Subject<Chirp>> chirpSubjects =
        new ConcurrentHashMap<>();
    private final Subject<Set<MessageID>> chirpsSubject =
        BehaviorSubject.createDefault(Collections.emptySet());

    // Reactions
    private final ConcurrentHashMap<MessageID, Set<Reaction>> reactionByChirpId =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MessageID, Reaction> reactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MessageID, Subject<Set<Reaction>>> reactionSubjectsByChirpId =
        new ConcurrentHashMap<>();

    public LaoChirps(SocialMediaRepository repository, String laoId) {
      this.repository = repository;
      this.laoId = laoId;
      loadStorage();
    }

    public void add(Chirp chirp) {
      MessageID id = chirp.getId();
      Chirp old = chirps.get(id);
      if (old != null) {
        Timber.tag(TAG).w("A chirp with id %s already exist : %s", id, old);
        return;
      }

      // Update repository data
      chirps.put(id, chirp);
      reactionByChirpId.putIfAbsent(chirp.getId(), ConcurrentHashMap.newKeySet());
      reactionSubjectsByChirpId.putIfAbsent(
          chirp.getId(), BehaviorSubject.createDefault(new HashSet<>()));

      // Publish new values on subjects
      chirpSubjects.put(id, BehaviorSubject.createDefault(chirp));
      chirpsSubject.toSerialized().onNext(new HashSet<>(chirps.keySet()));
    }

    public boolean addReaction(Reaction reaction) {
      // Check if the associated chirp is present
      Chirp chirp = chirps.get(reaction.getChirpId());
      if (chirp == null) {
        return false;
      }

      Set<Reaction> chirpReactions = Objects.requireNonNull(reactionByChirpId.get(chirp.getId()));

      // Search for a previous deleted reaction
      Reaction deleted = reactions.get(reaction.getId());
      if (deleted != null) {
        chirpReactions.remove(deleted);
      }

      // Update repository data
      reactions.put(reaction.getId(), reaction);
      chirpReactions.add(reaction);
      Objects.requireNonNull(reactionSubjectsByChirpId.get(chirp.getId()))
          .toSerialized()
          .onNext(new HashSet<>(chirpReactions));

      return true;
    }

    public boolean delete(MessageID id) {
      Chirp chirp = chirps.get(id);
      if (chirp == null) {
        return false;
      }

      if (chirp.isDeleted()) {
        Timber.tag(TAG).d("The chirp with id %s is already deleted", id);
      } else {
        Subject<Chirp> subject = chirpSubjects.get(id);
        if (subject == null) {
          // This should really never occurs
          throw new IllegalStateException("A chirp exist but has no associated subject with it");
        }

        Chirp deleted = chirp.deleted();
        chirps.put(id, deleted);
        subject.toSerialized().onNext(deleted);

        // Persist the deleted reaction (done only for completeness, this is not necessary)
        repository.disposables.add(
            repository
                .chirpDao
                .insert(new ChirpEntity(laoId, deleted))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () ->
                        Timber.tag(TAG)
                            .d("Successfully persisted deleted chirp %s", deleted.getId()),
                    err ->
                        Timber.tag(TAG)
                            .e(err, "Error in persisting deleted chirp %s", deleted.getId())));
      }
      return true;
    }

    public boolean deleteReaction(MessageID reactionId) {
      // Check if the associated reaction is present
      Reaction reaction = reactions.get(reactionId);
      if (reaction == null) {
        return false;
      }

      Chirp chirp = chirps.get(reaction.getChirpId());
      // If the chirp the reaction refers to it's not present then throw an error
      if (chirp == null) {
        throw new IllegalStateException("The reaction refers to a not existing chirp");
      }

      if (reaction.isDeleted()) {
        Timber.tag(TAG).d("The reaction with id %s is already deleted", reactionId);
      } else {
        // Update the repository data
        Reaction deleted = reaction.deleted();
        reactions.put(reactionId, deleted);
        Set<Reaction> chirpReactions = Objects.requireNonNull(reactionByChirpId.get(chirp.getId()));
        // Replace the old reaction with the deleted one
        chirpReactions.remove(reaction);
        chirpReactions.add(deleted);
        Objects.requireNonNull(reactionSubjectsByChirpId.get(chirp.getId()))
            .toSerialized()
            .onNext(chirpReactions);

        // Persist the deleted reaction (done only for completeness, this is not necessary)
        repository.disposables.add(
            repository
                .reactionDao
                .insert(new ReactionEntity(deleted))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () ->
                        Timber.tag(TAG)
                            .d("Successfully persisted deleted reaction %s", deleted.getId()),
                    err ->
                        Timber.tag(TAG)
                            .e(err, "Error in persisting deleted reaction %s", deleted.getId())));
      }

      return true;
    }

    public Observable<Set<MessageID>> getChirpsSubject() {
      return chirpsSubject;
    }

    public Observable<Chirp> getChirp(MessageID id) throws UnknownChirpException {
      Observable<Chirp> observable = chirpSubjects.get(id);
      if (observable == null) {
        throw new UnknownChirpException(id);
      } else {
        return observable;
      }
    }

    public Observable<Set<Reaction>> getReactions(MessageID chirpId) throws UnknownChirpException {
      Observable<Set<Reaction>> observable = reactionSubjectsByChirpId.get(chirpId);
      if (observable == null) {
        throw new UnknownChirpException(chirpId);
      }
      return observable;
    }

    /**
     * Load in memory the chirps and their respective reactions from the disk only when the user
     * wants to inflate the chirps adapter. It can be done only once per LAO, as during the
     * execution everything is also stored in memory.
     */
    private void loadStorage() {
      repository.disposables.add(
          repository
              .chirpDao
              .getChirpsByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  chirpsList ->
                      chirpsList.forEach(
                          chirp -> {
                            // Do not retrieve deleted chirps
                            if (chirp.isDeleted()) {
                              return;
                            }
                            // Load the chirp into the memory
                            add(chirp);
                            Timber.tag(TAG).d("Retrieved from db chirp %s", chirp.getId());
                            // When retrieving the chirp also retrieve its reactions
                            repository.disposables.add(
                                repository
                                    .reactionDao
                                    .getReactionsByChirpId(chirp.getId())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                        reactionsList ->
                                            reactionsList.forEach(
                                                reaction -> {
                                                  // Do not retrieve deleted reactions
                                                  if (reaction.isDeleted()) {
                                                    return;
                                                  }
                                                  // Load the reaction into the memory
                                                  addReaction(reaction);
                                                  Timber.tag(TAG)
                                                      .d(
                                                          "Retrieved from db reaction %s",
                                                          reaction.getId());
                                                }),
                                        err ->
                                            Timber.tag(TAG)
                                                .e(
                                                    err,
                                                    "No reaction found in the storage for chirp %s",
                                                    chirp.getId())));
                          }),
                  err ->
                      Timber.tag(TAG).e(err, "No chirp found in the storage for lao %s", laoId)));
    }
  }
}
