package com.github.dedis.popstellar.repository;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
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

  @Inject
  public SocialMediaRepository() {
    // Constructor required by Hilt
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
   * @param laoId of the lao we want to observe the chirp list
   * @return an observable set of message ids whose correspond to the set of chirp published on the
   *     given lao
   */
  @NonNull
  public Observable<Set<MessageID>> getChirpsOfLao(String laoId) {
    return getLaoChirps(laoId).getChirpsSubject();
  }

  @NonNull
  private synchronized LaoChirps getLaoChirps(String laoId) {
    // Create the lao chirps object if it is not present yet
    return chirpsByLao.computeIfAbsent(laoId, lao -> new LaoChirps());
  }

  /**
   * Add a reaction to a given chirp.
   *
   * @param laoId id of the lao the reaction was sent on
   * @param reaction reaction to add
   * @return true if the chirp associated with the given reaction exists and it's not deleted, false
   *     otherwise
   */
  public boolean addReaction(String laoId, Reaction reaction) {
    Timber.tag(TAG).d("Adding new reaction on lao %s : %s", laoId, reaction);
    // Retrieve Lao data and add the reaction to it
    return getLaoChirps(laoId).addReaction(reaction);
  }

  /**
   * Delete a reaction based on its id.
   *
   * @param laoId id of the lao the reaction was sent on
   * @param reactionID identifier of the reaction to delete
   * @return true if the reaction with the given id exists, false otherwise
   */
  public boolean deleteReaction(String laoId, MessageID reactionID) {
    Timber.tag(TAG).d("Deleting reaction on lao %s : %s", laoId, reactionID);
    // Retrieve Lao data and delete the reaction from it
    return getLaoChirps(laoId).deleteReaction(reactionID);
  }

  /**
   * This class holds the social media data of a specific lao
   *
   * <p>Its purpose is to hold data in a way that it is easier to handle and understand. It is also
   * a way to avoid any conflict between laos.
   */
  private static final class LaoChirps {

    private final Map<MessageID, Chirp> chirps = new HashMap<>();
    private final Map<MessageID, Subject<Chirp>> chirpSubjects = new HashMap<>();
    private final Subject<Set<MessageID>> chirpsSubject =
        BehaviorSubject.createDefault(Collections.emptySet());

    private final Map<MessageID, Set<MessageID>> reactionIdsByChirpId = new HashMap<>();
    private final Map<MessageID, Reaction> reactions = new HashMap<>();

    public synchronized void add(Chirp chirp) {
      MessageID id = chirp.getId();
      Chirp old = chirps.get(id);
      if (old != null) {
        Timber.tag(TAG).w("A chirp with id %s already exist : %s", id, old);
        return;
      }

      // Update repository data
      chirps.put(id, chirp);

      // Publish new values on subjects
      chirpSubjects.put(id, BehaviorSubject.createDefault(chirp));
      chirpsSubject.onNext(chirps.keySet());
    }

    public synchronized boolean addReaction(Reaction reaction) {
      // Check if the associated chirp is present and not deleted
      Chirp chirp = chirps.get(reaction.getChirpId());
      if (chirp == null || chirp.isDeleted()) {
        return false;
      }

      // Update repository data
      reactionIdsByChirpId.putIfAbsent(chirp.getId(), new HashSet<>());
      reactionIdsByChirpId.get(chirp.getId()).add(reaction.getId());
      reactions.put(reaction.getId(), reaction);

      return true;
    }

    public synchronized boolean delete(MessageID id) {
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
        subject.onNext(deleted);
      }
      return true;
    }

    public synchronized boolean deleteReaction(MessageID reactionId) {
      // Check if the associated reaction is present
      Reaction reaction = reactions.get(reactionId);
      if (reaction == null) {
        return false;
      }

      Chirp chirp = chirps.get(reaction.getChirpId());

      // Update the repository data
      reactionIdsByChirpId.get(chirp.getId()).remove(reaction.getId());
      reactions.remove(reactionId);

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
  }
}
