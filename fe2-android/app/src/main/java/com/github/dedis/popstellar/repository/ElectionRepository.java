package com.github.dedis.popstellar.repository;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao;
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import java.util.*;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

/**
 * This class is the repository of the elections events
 *
 * <p>Its main purpose is to store elections and publish updates
 */
@Singleton
public class ElectionRepository {

  private static final String TAG = ElectionRepository.class.getSimpleName();

  private final Map<String, LaoElections> electionsByLao = new HashMap<>();

  private final ElectionDao electionDao;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public ElectionRepository(AppDatabase appDatabase, Application application) {
    electionDao = appDatabase.electionDao();
    Map<Lifecycle.Event, Consumer<Activity>> consumerMap = new EnumMap<>(Lifecycle.Event.class);
    consumerMap.put(Lifecycle.Event.ON_STOP, activity -> disposables.clear());
    application.registerActivityLifecycleCallbacks(
        ActivityUtils.buildLifecycleCallback(consumerMap));
  }

  /**
   * Update the election state.
   *
   * <p>It can be a new election or an update.
   *
   * @param election the election to update
   */
  public void updateElection(@NonNull Election election) {
    ElectionEntity electionEntity = new ElectionEntity(election);
    // Persist the election
    disposables.add(
        electionDao
            .insert(electionEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> Timber.tag(TAG).d("Successfully persisted election %s", election.getId()),
                err ->
                    Timber.tag(TAG).e(err, "Error in persisting election %s", election.getId())));
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
  public Election getElection(@NonNull String laoId, @NonNull String electionId)
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
  public Observable<Set<Election>> getElectionsObservableInLao(@NonNull String laoId) {
    return getLaoElections(laoId).getElectionsSubject();
  }

  @NonNull
  private synchronized LaoElections getLaoElections(String laoId) {
    // Create the lao elections object if it is not present yet
    return electionsByLao.computeIfAbsent(laoId, lao -> new LaoElections(this, laoId));
  }

  private static final class LaoElections {
    private final ElectionRepository repository;
    private final String laoId;
    private boolean alreadyRetrieved = false;

    private final Map<String, Election> electionById = new HashMap<>();
    private final Map<String, Subject<Election>> electionSubjects = new HashMap<>();
    private final BehaviorSubject<Set<Election>> electionsSubject =
        BehaviorSubject.createDefault(Collections.emptySet());

    public LaoElections(ElectionRepository repository, String laoId) {
      this.repository = repository;
      this.laoId = laoId;
    }

    public synchronized void updateElection(@NonNull Election election) {
      String id = election.getId();

      electionById.put(id, election);
      electionSubjects.putIfAbsent(id, BehaviorSubject.create());
      Objects.requireNonNull(electionSubjects.get(id)).onNext(election);

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
      loadStorage();
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

    /**
     * Load in memory the elections from the disk only when the user clicks on the respective LAO,
     * just needed one time only.
     */
    private void loadStorage() {
      if (alreadyRetrieved) {
        return;
      }
      repository.disposables.add(
          repository
              .electionDao
              .getElectionsByLaoId(laoId, electionById.keySet())
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  elections ->
                      elections.forEach(
                          election -> {
                            updateElection(election);
                            Timber.tag(TAG).d("Retrieved from db election %s", election.getId());
                          }),
                  err ->
                      Timber.tag(TAG)
                          .e(err, "No election found in the storage for lao %s", laoId)));
      alreadyRetrieved = true;
    }
  }
}
