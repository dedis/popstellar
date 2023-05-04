package com.github.dedis.popstellar.repository;

import android.annotation.SuppressLint;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

@Singleton
public class LAORepository {

  private static final String TAG = LAORepository.class.getSimpleName();

  private final LAODao laoDao;

  private final ConcurrentHashMap<String, Lao> laoById = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Subject<LaoView>> subjectById = new ConcurrentHashMap<>();
  private final BehaviorSubject<List<String>> laosSubject = BehaviorSubject.create();

  // ============ Lao Unrelated data ===============
  // State for Messages
  // Observable for view models that need access to all Nodes
  private final Map<Channel, BehaviorSubject<List<ConsensusNode>>> channelToNodesSubject =
      new HashMap<>();

  @Inject
  public LAORepository(AppDatabase appDatabase) {
    this.laoDao = appDatabase.laoDao();
    loadPersistentStorage();
  }

  /**
   * This functions is called on start to load all the laos in memory as they must be displayed in
   * the home list. Given the fact that we load every lao in memory at the beginning a cache is not
   * necessary. This is also possible memory-wise as usually the number of laos is very limited.
   * This call is asynchronous so it's performed in background not blocking the main thread.
   */
  @SuppressLint("CheckResult")
  private void loadPersistentStorage() {
    laoDao
        .getAllLaos()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            laos -> {
              if (laos.isEmpty()) {
                Timber.tag(TAG).d("No LAO has been found in the database");
                return;
              }
              laos.forEach(
                  lao -> {
                    laoById.put(lao.getId(), lao);
                    subjectById.put(lao.getId(), BehaviorSubject.createDefault(new LaoView(lao)));
                  });
              List<String> laoIds = laos.stream().map(Lao::getId).collect(Collectors.toList());
              laosSubject.onNext(laoIds);
              Timber.tag(TAG).d("Loaded all the LAOs from database: %s", laoIds);
            });
  }

  /**
   * Retrieves the Lao in a given channel
   *
   * @param channel the channel on which the Lao was created
   * @return the Lao corresponding to this channel
   */
  public Lao getLaoByChannel(Channel channel) {
    Timber.tag(TAG).d("querying lao for channel %s", channel);
    return laoById.get(channel.extractLaoId());
  }

  /**
   * Checks that a LAO with the given id exists in the repo
   *
   * @param laoId the LAO id to check
   * @return true if a LAO with the given id exists
   */
  public boolean containsLao(String laoId) {
    return laoById.containsKey(laoId);
  }

  public Observable<List<String>> getAllLaoIds() {
    return laosSubject;
  }

  public Observable<LaoView> getLaoObservable(String laoId) {
    subjectById.computeIfAbsent(laoId, id -> BehaviorSubject.create());
    return subjectById.get(laoId);
  }

  public LaoView getLaoView(String id) throws UnknownLaoException {
    Lao lao = laoById.get(id);
    if (lao == null) {
      // In some Android devices after putting the application in the
      // background or locking the screen it happens that the lao is not found.
      // This could be due to the ram being cleared, so a I/O check could help avoiding this
      // scenario.
      Lao laoFromDb = laoDao.getLaoById(id);
      if (laoFromDb == null) {
        throw new UnknownLaoException(id);
      } else {
        // Restore the lao
        updateLao(laoFromDb);
      }
    }

    return new LaoView(lao);
  }

  public LaoView getLaoViewByChannel(Channel channel) throws UnknownLaoException {
    return getLaoView(channel.extractLaoId());
  }

  public synchronized void updateLao(Lao lao) {
    Timber.tag(TAG).d("Updating Lao %s", lao);
    if (lao == null) {
      throw new IllegalArgumentException();
    }

    LaoView laoView = new LaoView(lao);
    LAOEntity laoEntity = new LAOEntity(lao.getId(), lao);

    // Update the persistent storage in background (replace if already existing)
    laoDao
        .insert(laoEntity)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete(() -> Timber.tag(TAG).d("Persisted Lao %s", lao))
        .subscribe();

    if (laoById.containsKey(lao.getId())) {
      // If the lao already exists, we can push the next update
      laoById.put(lao.getId(), lao);
      // Update observer if present
      Subject<LaoView> subject = subjectById.get(lao.getId());
      if (subject != null) {
        subject.onNext(laoView);
      }
    } else {
      // Otherwise, create the entry
      laoById.put(lao.getId(), lao);
      // Update lao list
      laosSubject.onNext(new ArrayList<>(laoById.keySet()));
      subjectById.put(lao.getId(), BehaviorSubject.createDefault(laoView));
    }
  }

  /**
   * This function removes from the home all the laos displayed when the user wants to clear the
   * data storage. The maps are automatically cleared by the intent flags.
   */
  public void clearRepository() {
    Timber.tag(TAG).d("Clearing LAORepository");
    laosSubject.onNext(new ArrayList<>());
  }

  // ============ Lao Unrelated functions ===============

  /**
   * Return an Observable to the list of nodes in a given channel.
   *
   * @param channel the lao channel.
   * @return an Observable to the list of nodes
   */
  public Observable<List<ConsensusNode>> getNodesByChannel(Channel channel) {
    return channelToNodesSubject.get(channel);
  }

  /**
   * Emit an update to the observer of nodes for the given lao channel. Create the BehaviorSubject
   * if absent (first update).
   *
   * @param channel the lao channel
   */
  public void updateNodes(Channel channel) {
    List<ConsensusNode> nodes = getLaoByChannel(channel).getNodes();
    channelToNodesSubject.putIfAbsent(channel, BehaviorSubject.create());
    channelToNodesSubject.get(channel).onNext(nodes);
  }
}
