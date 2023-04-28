package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;

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

  private final Map<String, Lao> laoById = new HashMap<>();

  private final Map<String, Subject<LaoView>> subjectById = new HashMap<>();
  private final BehaviorSubject<List<String>> laosSubject = BehaviorSubject.create();

  // ============ Lao Unrelated data ===============
  // State for Messages
  // Observable for view models that need access to all Nodes
  private final Map<Channel, BehaviorSubject<List<ConsensusNode>>> channelToNodesSubject =
      new HashMap<>();

  @Inject
  public LAORepository(AppDatabase appDatabase) {
    laoDao = appDatabase.laoDao();
    loadPersistentStorage();
  }

  private void loadPersistentStorage() {
    // On start load all the laos in the memory
    // Cache isn't strictly needed as usually there are a few laos
    List<LAOEntity> laos = laoDao.getAllLaos();
    if (laos != null) {
      List<String> ids = new ArrayList<>();
      laos.forEach(
          lao -> {
            laoById.put(lao.getLaoId(), lao.getLao());
            subjectById.put(
                lao.getLaoId(), BehaviorSubject.createDefault(new LaoView(lao.getLao())));
            ids.add(lao.getLaoId());
          });
      laosSubject.onNext(ids);
    }
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
      throw new UnknownLaoException(id);
    }

    return new LaoView(lao);
  }

  public LaoView getLaoViewByChannel(Channel channel) throws UnknownLaoException {
    return getLaoView(channel.extractLaoId());
  }

  public synchronized void updateLao(Lao lao) {
    Timber.tag(TAG).d("updating Lao %s", lao);
    if (lao == null) {
      throw new IllegalArgumentException();
    }
    LaoView laoView = new LaoView(lao);

    LAOEntity laoEntity = new LAOEntity(lao.getId(), lao);
    // Update the persistent storage (replace if already existing)
    laoDao
        .insert(laoEntity)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
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

  public void deleteRepository() {
    laoById.clear();
    subjectById.clear();
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
