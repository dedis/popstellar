package com.github.dedis.popstellar.repository;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

@Singleton
public class LAORepository {

  private static final String TAG = LAORepository.class.getSimpleName();
  private static final String ROOT = "/root/";

  // State for LAO
  private final Map<String, LAOState> laoById;

  // State for Messages
  private final Map<MessageID, MessageGeneral> messageById;

  // Observable for view models that need access to all LAO Names
  private final BehaviorSubject<List<Lao>> allLaoSubject;

  // Observable for view models that need access to all Nodes
  private final Map<String, BehaviorSubject<List<ConsensusNode>>> channelToNodesSubject;

  @Inject
  public LAORepository() {
    laoById = new HashMap<>();
    messageById = new HashMap<>();
    allLaoSubject = BehaviorSubject.create();
    channelToNodesSubject = new HashMap<>();
  }

  /**
   * Checks that a given channel corresponds to a LAO channel, i.e /root/laoId
   *
   * @param channel the channel we want to check
   * @return true if the channel is a lao channel, false otherwise
   */
  public boolean isLaoChannel(String channel) {
    return channel.split("/").length == 3;
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
  public Election getElectionByChannel(String channel) {
    Log.d(TAG, "querying election for channel " + channel);

    if (channel.split("/").length < 4)
      throw new IllegalArgumentException("invalid channel for an election : " + channel);

    Lao lao = getLaoByChannel(channel);
    Optional<Election> electionOption = lao.getElection(channel.split("/")[3]);
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
  public Lao getLaoByChannel(String channel) {
    Log.d(TAG, "querying lao for channel " + channel);

    String[] split = channel.split("/");
    return laoById.get(ROOT + split[2]).getLao();
  }

  public Observable<List<Lao>> getAllLaos() {
    return allLaoSubject;
  }

  public Observable<Lao> getLaoObservable(String channel) {
    Log.d(TAG, "LaoIds we have are: " + laoById.keySet());
    return laoById.get(channel).getObservable();
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
  public Observable<List<ConsensusNode>> getNodesByChannel(String channel) {
    return channelToNodesSubject.get(channel);
  }

  /**
   * Emit an update to the observer of nodes for the given lao channel. Create the BehaviorSubject
   * if absent (first update).
   *
   * @param channel the lao channel
   */
  public void updateNodes(String channel) {
    List<ConsensusNode> nodes = getLaoByChannel(channel).getNodes();
    channelToNodesSubject.putIfAbsent(channel, BehaviorSubject.create());
    channelToNodesSubject.get(channel).onNext(nodes);
  }

  public Map<MessageID, MessageGeneral> getMessageById() {
    return messageById;
  }
}
