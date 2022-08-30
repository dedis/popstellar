package com.github.dedis.popstellar.repository;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

@Singleton
public class LAORepository {

  private static final String TAG = LAORepository.class.getSimpleName();

  private final HashMap<String, Lao> laoById = new HashMap<>();
  private final HashMap<String, Subject<Lao>> subjectById = new HashMap<>();
  private final BehaviorSubject<List<String>> laosSubject = BehaviorSubject.create();

  // ============ Lao Unrelated data ===============
  // State for Messages
  private final Map<MessageID, MessageGeneral> messageById = new HashMap<>();
  // Observable for view models that need access to all Nodes
  private final Map<Channel, BehaviorSubject<List<ConsensusNode>>> channelToNodesSubject =
      new HashMap<>();

  @Inject
  public LAORepository() {
    // Constructor required by Hilt
  }

  /**
   * Retrieves the Election in a given channel
   *
   * @param channel the channel on which the election was created
   * @return the election corresponding to this channel
   */
  public Election getElectionByChannel(Channel channel) {
    Log.d(TAG, "querying election for channel " + channel);

    if (!channel.isElectionChannel())
      throw new IllegalArgumentException("invalid channel for an election : " + channel);

    Lao lao = getLaoByChannel(channel);
    Optional<Election> electionOption = lao.getElection(channel.extractElectionId());
    if (!electionOption.isPresent()) {
      throw new IllegalArgumentException("the election should be present when receiving a result");
    }
    return electionOption.get();
  }

  /**
   * Retrieves the Lao in a given channel
   *
   * @param channel the channel on which the Lao was created
   * @return the Lao corresponding to this channel
   */
  public Lao getLaoByChannel(Channel channel) {
    Log.d(TAG, "querying lao for channel " + channel);
    return laoById.get(channel.extractLaoId());
  }

  public Observable<List<String>> getAllLaoIds() {
    return laosSubject;
  }

  public Observable<Lao> getLaoObservable(String laoId) {
    Log.d(TAG, "LaoIds we have are: " + laoById.keySet());
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
    if (lao == null) {
      throw new IllegalArgumentException();
    }

    if (laoById.containsKey(lao.getId())) {
      // If the lao already exists, we can push the next update
      laoById.put(lao.getId(), lao);
      // Update observer if present
      Subject<Lao> subject = subjectById.get(lao.getId());
      if (subject != null) {
        subject.onNext(lao);
      }
    } else {
      // Otherwise, create the entry
      laoById.put(lao.getId(), lao);
      // Update lao list
      laosSubject.onNext(new ArrayList<>(laoById.keySet()));
      subjectById.put(lao.getId(), BehaviorSubject.createDefault(lao));
    }
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

  public Map<MessageID, MessageGeneral> getMessageById() {
    return messageById;
  }
}
