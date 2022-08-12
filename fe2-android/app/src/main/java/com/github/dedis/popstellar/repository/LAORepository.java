package com.github.dedis.popstellar.repository;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

@Singleton
public class LAORepository {

  private static final String TAG = LAORepository.class.getSimpleName();

  // State for LAO
  private final Map<String, LAOState> laoById;

  // State for Messages
  private final Map<MessageID, MessageGeneral> messageById;

  // Observable for view models that need access to all LAO Names
  private final BehaviorSubject<List<Lao>> allLaoSubject;

  // Observable for view models that need access to all Nodes
  private final Map<Channel, BehaviorSubject<List<ConsensusNode>>> channelToNodesSubject;

  @Inject
  public LAORepository() {
    laoById = new HashMap<>();
    messageById = new HashMap<>();
    allLaoSubject = BehaviorSubject.create();
    channelToNodesSubject = new HashMap<>();
  }

  /** Set allLaoSubject to contain all LAOs */
  public void setAllLaoSubject() {
    Log.d(TAG, "posted allLaos to allLaoSubject");
    allLaoSubject.onNext(
        laoById.values().stream().map(LAOState::getLao).collect(Collectors.toList()));
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
    return laoById.get(channel.extractLaoId()).getLao();
  }

  public Observable<List<Lao>> getAllLaos() {
    return allLaoSubject;
  }

  public Observable<Lao> getLaoObservable(String laoId) {
    Log.d(TAG, "LaoIds we have are: " + laoById.keySet());
    return laoById.get(laoId).getObservable();
  }

  public Map<String, LAOState> getLaoById() {
    return laoById;
  }

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

  public Optional<LaoView> getLao(String id) {
    if (laoById.containsKey(id)) {
      return Optional.of(new LaoView(laoById.get(id).getLao()));
    }
    return Optional.empty();
  }

  public LaoView getLaoViewByChannel(Channel channel) throws UnknownLaoException {
    return getLao(channel.extractLaoId())
        .orElseThrow(() -> new UnknownLaoException(channel.extractLaoId()));
  }

  public synchronized void updateLao(Lao lao) {
    if (lao == null) {
      throw new IllegalArgumentException();
    }

    if (laoById.containsKey(lao.getId())) {
      // If the lao already exists, we can push the next update
      laoById.get(lao.getId()).publish(lao);
    } else {
      // Otherwise, create the state
      laoById.put(lao.getId(), new LAOState(lao));
    }
  }
}
