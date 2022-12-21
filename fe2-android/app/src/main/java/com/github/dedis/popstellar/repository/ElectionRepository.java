package com.github.dedis.popstellar.repository;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import java.util.*;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class ElectionRepository {

  private final Map<String, LaoElections> electionsByLao = new HashMap<>();

  @Inject
  public ElectionRepository() {
    // Constructor required by Hilt
  }

  public void updateElection(@NonNull String laoId, @NonNull Election election) {
    getLaoElections(laoId).updateElection(election);
  }

  @NonNull
  public Election getElection(@NonNull String laoId, @NonNull String electionId)
      throws UnknownElectionException {
    return getLaoElections(laoId).getElection(electionId);
  }

  public Observable<Election> getElectionObservable(
      @NonNull String laoId, @NonNull String electionId) throws UnknownElectionException {
    return getLaoElections(laoId).getElectionSubject(electionId);
  }

  @NonNull
  public Observable<Set<String>> getElectionsObservable(@NonNull String laoId) {
    return getLaoElections(laoId).getElectionsSubject();
  }

  @NonNull
  private synchronized LaoElections getLaoElections(String laoId) {
    // Create the lao chirps object if it is not present yet
    return electionsByLao.computeIfAbsent(laoId, lao -> new LaoElections());
  }

  @NonNull
  public Election getElectionByChannel(Channel channel) throws UnknownElectionException {
    return getElection(channel.extractLaoId(), channel.extractElectionId());
  }

  private static final class LaoElections {
    private final Map<String, Election> electionById = new HashMap<>();
    private final Map<String, Subject<Election>> electionSubjects = new HashMap<>();
    private final BehaviorSubject<Set<String>> electionsSubject =
        BehaviorSubject.createDefault(Collections.emptySet());

    public synchronized void updateElection(@NonNull Election election) {
      String id = election.getId();

      electionById.put(id, election);
      electionSubjects.putIfAbsent(id, BehaviorSubject.create());
      //noinspection ConstantConditions
      electionSubjects.get(id).onNext(election);

      electionsSubject.onNext(electionById.keySet());
    }

    public Election getElection(@NonNull String electionId) throws UnknownElectionException {

      Election election = electionById.get(electionId);

      if (election == null) {
        throw new UnknownElectionException(electionId);
      } else {
        return election;
      }
    }

    public Observable<Set<String>> getElectionsSubject() {
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
