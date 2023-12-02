package com.github.dedis.popstellar.repository;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConsensusRepository {

  private final Map<String, LaoConsensus> consensusByLao = new HashMap<>();
  private final Map<Channel, BehaviorSubject<List<ConsensusNode>>> channelToNodesSubject =
      new HashMap<>();

  @Inject
  public ConsensusRepository() {
    // Required empty constructor
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
  public void updateNodesByChannel(Channel channel) {
    List<ConsensusNode> nodes = getLaoConsensus(channel.extractLaoId()).getNodes();
    channelToNodesSubject.putIfAbsent(channel, BehaviorSubject.create());
    channelToNodesSubject.get(channel).onNext(nodes);
  }

  public List<ConsensusNode> getNodes(String laoId) {
    return getLaoConsensus(laoId).getNodes();
  }

  public void updateElectInstanceByLao(String laoId, @NonNull ElectInstance electInstance) {
    getLaoConsensus(laoId).updateElectInstance(electInstance);
  }

  public Optional<ElectInstance> getElectInstance(String laoId, MessageID messageId) {
    // TODO uncomment that when consensus does not rely on call by reference
    //    Optional<ElectInstance> optional = getLaoConsensus(laoId).getElectInstance(messageId);
    //    return optional.map(ElectInstance::new); // If empty returns empty optional, if not
    //    returns optional with copy of retrieved ElectInstance
    return getLaoConsensus(laoId).getElectInstance(messageId);
  }

  public void setOrganizer(String laoId, PublicKey organizer) {
    getLaoConsensus(laoId).setOrganizer(organizer);
  }

  public void initKeyToNode(String laoId, Set<PublicKey> witnesses) {
    getLaoConsensus(laoId).initKeyToNode(witnesses);
  }

  public ConsensusNode getNodeByLao(String laoId, @NonNull PublicKey key) {
    return getLaoConsensus(laoId).getNode(key);
  }

  public Map<MessageID, ElectInstance> getMessageIdToElectInstanceByLao(String laoId) {
    return getLaoConsensus(laoId).getMessageIdToElectInstance();
  }

  /** Get in a thread-safe fashion the consensus object for the lao, computes it if absent. */
  @NonNull
  private synchronized LaoConsensus getLaoConsensus(String laoId) {
    // Create the lao consensus object if it is not present yet
    return consensusByLao.computeIfAbsent(laoId, lao -> new LaoConsensus());
  }

  private static class LaoConsensus {
    private final Map<MessageID, ElectInstance> messageIdToElectInstance = new HashMap<>();
    private final Map<PublicKey, ConsensusNode> keyToNode = new HashMap<>();

    /**
     * Store the given ElectInstance and update all nodes concerned by it.
     *
     * @param electInstance the ElectInstance
     */
    private void updateElectInstance(@NonNull ElectInstance electInstance) {
      MessageID messageId = electInstance.getMessageId();
      messageIdToElectInstance.put(messageId, electInstance);

      Map<PublicKey, MessageID> acceptorsToMessageId = electInstance.getAcceptorsToMessageId();
      // add to each node the messageId of the Elect if they accept it
      keyToNode.forEach(
          (key, node) -> {
            if (acceptorsToMessageId.containsKey(key)) {
              node.addMessageIdOfAnAcceptedElect(messageId);
            }
          });

      // add the ElectInstance to the proposer node
      ConsensusNode proposer = keyToNode.get(electInstance.getProposer());
      if (proposer != null) {
        proposer.addElectInstance(electInstance);
      }
    }

    private Optional<ElectInstance> getElectInstance(MessageID messageId) {
      return Optional.ofNullable(messageIdToElectInstance.get(messageId));
    }

    private void setOrganizer(PublicKey organizer) {
      keyToNode.computeIfAbsent(organizer, ConsensusNode::new);
    }

    /**
     * Get the list of all nodes of this Lao sorted by the base64 representation of their public
     * key.
     *
     * @return a sorted List of ConsensusNode
     */
    private List<ConsensusNode> getNodes() {
      List<ConsensusNode> nodes = new ArrayList<>(keyToNode.values());
      nodes.sort(Comparator.comparing(node -> node.getPublicKey().getEncoded()));
      return nodes;
    }

    private void initKeyToNode(Set<PublicKey> witnesses) {
      if (witnesses == null) {
        throw new IllegalArgumentException("The witnesses set is null");
      }
      for (PublicKey witness : witnesses) {
        if (witness == null) {
          throw new IllegalArgumentException("One of the witnesses in the set is null");
        }
      }
      witnesses.forEach(w -> keyToNode.computeIfAbsent(w, ConsensusNode::new));
    }

    private ConsensusNode getNode(@NonNull PublicKey key) {
      return keyToNode.get(key);
    }

    private Map<MessageID, ElectInstance> getMessageIdToElectInstance() {
      return Collections.unmodifiableMap(messageIdToElectInstance);
    }
  }
}
