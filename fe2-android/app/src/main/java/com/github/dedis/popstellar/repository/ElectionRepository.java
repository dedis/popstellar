package com.github.dedis.popstellar.repository;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * This class is the repository of the elections events
 *
 * <p>Its main purpose is to store elections and publish updates
 */
@Singleton
public class ElectionRepository {

  private final Map<String, LaoElections> electionsByLao = new HashMap<>();

  @Inject
  public ElectionRepository() {
    // Constructor required by Hilt
  }

  /**
   * Update the election state.
   *
   * <p>It can be a new election or an update.
   *
   * @param election the election to update
   */
  public void updateElection(@NonNull Election election) {
    getLaoElections(election.getChannel().extractLaoId()).updateElection(election);
  }

  /**
   * Retrieve an election state given its Lao and its ID
   *
   * @param laoId of the lao the election is part of
   * @param electionId id of the election
   * @return the election state
   * @throws UnknownElectionException if no election with this id exist in this lao
   */
  @NonNull
  public com.github.dedis.popstellar.model.objects.Election getElection(@NonNull String laoId, @NonNull String electionId)
      throws UnknownElectionException {
    return getLaoElections(laoId).getElection(electionId);
  }

  /**
   * Retrieve an election state given its channel
   *
   * @param channel of the election
   * @return the election state
   * @throws UnknownElectionException if no election with this id exist in the lao
   */
  @NonNull
  public Election getElectionByChannel(@NonNull Channel channel) throws UnknownElectionException {
    return getElection(channel.extractLaoId(), channel.extractElectionId());
  }

  /**
   * Retrieve an election observable given its Lao and its ID
   *
   * <p>The observable will carry any update made fow this election
   *
   * @param laoId of the lao the election is part of
   * @param electionId id of the election
   * @return the election observable
   * @throws UnknownElectionException if no election with this id exist in this lao
   */
  public Observable<Election> getElectionObservable(
      @NonNull String laoId, @NonNull String electionId) throws UnknownElectionException {
    return getLaoElections(laoId).getElectionSubject(electionId);
  }

  /**
   * Compute an Observable containing the set of ids of all election in the given Lao
   *
   * @param laoId lao of the elections
   * @return an observable that will be updated with the set of all election's ids
   */
  @NonNull
  public Observable<Set<Election>> getElectionsObservable(@NonNull String laoId) {
    return getLaoElections(laoId).getElectionsSubject();
  }

  @NonNull
  private synchronized LaoElections getLaoElections(String laoId) {
    // Create the lao elections object if it is not present yet
    return electionsByLao.computeIfAbsent(laoId, lao -> new LaoElections());
  }

  private static final class LaoElections {
    private final Map<String, Election> electionById = new HashMap<>();
    private final Map<String, Subject<Election>> electionSubjects = new HashMap<>();
    private final BehaviorSubject<Set<Election>> electionsSubject =
        BehaviorSubject.createDefault(Collections.emptySet());

    public synchronized void updateElection(@NonNull Election election) {
      String id = election.getId();

      electionById.put(id, election);
      electionSubjects.putIfAbsent(id, BehaviorSubject.create());
      //noinspection ConstantConditions
      electionSubjects.get(id).onNext(election);

      electionsSubject.onNext(Collections.unmodifiableSet(new HashSet<>(electionById.values())));
    }

    public Election getElection(@NonNull String electionId) throws UnknownElectionException {
      Election election = electionById.get(electionId);

      if (election == null) {
        throw new UnknownElectionException(electionId);
      } else {
        return election;
      }
    }

    public Observable<Set<Election>> getElectionsSubject() {
      return electionsSubject;
    }

    public Observable<Election> getElectionSubject(@NonNull String electionId)
        throws UnknownElectionException {
      Observable<Election> electionObservable = electionSubjects.get(electionId);

      if (electionObservable == null) {
        throw new UnknownElectionException(electionId);
      } else {
        return electionObservable;
      }
    }
  }
}
