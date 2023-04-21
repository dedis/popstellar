package com.github.dedis.popstellar.repository;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * This class is the repository of the social media feature
 *
 * <p>Its main purpose is to store chirps and publish updates
 */
@Singleton
public class SocialMediaRepository {

  private static final Logger logger = LogManager.getLogger(SocialMediaRepository.class);

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
    logger.debug("Adding new chirp on lao " + laoId + " : " + chirp);
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
    logger.debug("Deleting chirp on lao " + laoId + " with id " + id);
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

    public synchronized void add(Chirp chirp) {
      MessageID id = chirp.getId();
      Chirp old = chirps.get(id);
      if (old != null) {
        logger.warn("A chirp with id " + id + " already exist : " + old);
        return;
      }

      // Update repository data
      chirps.put(id, chirp);

      // Publish new values on subjects
      chirpSubjects.put(id, BehaviorSubject.createDefault(chirp));
      chirpsSubject.onNext(chirps.keySet());
    }

    public synchronized boolean delete(MessageID id) {
      Chirp chirp = chirps.get(id);
      if (chirp == null) {
        return false;
      }

      if (chirp.isDeleted()) {
        logger.debug("The chirp with id " + id + " is already deleted");
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
