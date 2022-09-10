package com.github.dedis.popstellar.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;

import java.util.*;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * The class is the repository of the social media feature
 *
 * <p>Its main purpose is to store chirps and publish updates
 */
public class SocialMediaRepository {

  private static final String TAG = SocialMediaRepository.class.getSimpleName();

  // Data of the repository
  private final Map<MessageID, Chirp> chirpById = new HashMap<>();
  private final Map<String, Set<MessageID>> chirpByLao = new HashMap<>();
  // Observables of the data
  private final Map<MessageID, Subject<Chirp>> chirpSubjects = new HashMap<>();
  private final Map<String, Subject<Set<MessageID>>> chirpsByLaoSubjects = new HashMap<>();

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
  public synchronized void addChirp(String laoId, Chirp chirp) {
    Log.d(TAG, "Adding new chirp on lao " + laoId + " : " + chirp);
    MessageID id = chirp.getId();

    Chirp old = chirpById.get(chirp.getId());
    if (old != null) {
      Log.w(TAG, "A chirp with id " + id + " already exist : " + old);
    }

    // Update repository data
    chirpById.put(id, chirp);
    Set<MessageID> ids = setOf(chirpByLao.get(laoId), id);
    chirpByLao.put(laoId, ids);

    // Publish new values
    chirpSubjects.put(id, BehaviorSubject.createDefault(chirp));
    chirpsByLaoSubjects.compute(
        laoId,
        (lao, subject) -> {
          if (subject == null) {
            // Create subject if it was not present
            return BehaviorSubject.createDefault(ids);
          } else {
            // else, simply publish
            subject.onNext(ids);
            return subject;
          }
        });
  }

  private Set<MessageID> setOf(Set<MessageID> ids, MessageID id) {
    Set<MessageID> newValue = new HashSet<>();
    if (ids != null) newValue.addAll(ids);
    newValue.add(id);

    return newValue;
  }

  /**
   * Delete a chirp based on its id
   *
   * @param id of the chirp to delete
   * @return true is a chirp with given id existed
   */
  public synchronized boolean deleteChirp(MessageID id) {
    Chirp chirp = chirpById.get(id);
    if (chirp == null) {
      return false;
    }

    if (chirp.getIsDeleted()) {
      Log.d(TAG, "The chirp with id " + id + " is already deleted");
    } else {
      chirp.setIsDeleted(true);
      Subject<Chirp> subject = chirpSubjects.get(id);
      if (subject == null) {
        throw new IllegalStateException("A chirp exist but has no associated subject with it");
      }

      subject.onNext(chirp);
    }
    return true;
  }

  /**
   * @return the observable of a specific chirp
   */
  @NonNull
  public synchronized Observable<Chirp> getChirps(MessageID id) throws UnknownChirpException {
    Observable<Chirp> observable = chirpSubjects.get(id);
    if (observable == null) throw new UnknownChirpException(id);
    return observable;
  }

  /**
   * @param laoId of the lao we want to observe the chirp list
   * @return an observable set of message ids whose correspond to the set of chirp published on the
   *     given lao
   */
  @NonNull
  public synchronized Observable<Set<MessageID>> getChirpsOfLao(String laoId) {
    Subject<Set<MessageID>> subject = chirpsByLaoSubjects.get(laoId);
    if (subject == null) {
      // Create the subject even if the lao has no chirp yet
      subject = BehaviorSubject.createDefault(new HashSet<>());
      chirpsByLaoSubjects.put(laoId, subject);
    }

    return subject;
  }
}
