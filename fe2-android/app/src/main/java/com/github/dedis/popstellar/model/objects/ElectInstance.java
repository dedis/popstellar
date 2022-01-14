package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ElectInstance {

  private final MessageID messageId;
  private final String channel;
  private final PublicKey proposer;
  private final ConsensusElect elect;
  private final Set<PublicKey> nodes;
  // map the public key of acceptors to the id of their ElectAccept message
  private final Map<PublicKey, MessageID> acceptorToMessageId;

  private State state;

  public ElectInstance(
      @NonNull MessageID messageID,
      @NonNull String channel,
      @NonNull PublicKey proposer,
      @NonNull Set<PublicKey> nodes,
      @NonNull ConsensusElect elect) {
    this.messageId = messageID;
    this.channel = channel;
    this.proposer = proposer;
    this.elect = elect;
    this.nodes = Collections.unmodifiableSet(nodes);
    this.acceptorToMessageId = new HashMap<>();

    this.state = State.STARTING;
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public String getChannel() {
    return channel;
  }

  public String getInstanceId() {
    return elect.getInstanceId();
  }

  public ConsensusKey getKey() {
    return elect.getKey();
  }

  public Object getValue() {
    return elect.getValue();
  }

  public long getCreation() {
    return elect.getCreation();
  }

  public State getState() {
    return state;
  }

  public void setState(@NonNull State state) {
    this.state = state;
  }

  public PublicKey getProposer() {
    return proposer;
  }

  public Map<PublicKey, MessageID> getAcceptorsToMessageId() {
    return Collections.unmodifiableMap(acceptorToMessageId);
  }

  public void addElectAccept(
      @NonNull PublicKey publicKey,
      @NonNull MessageID messageID,
      @NonNull ConsensusElectAccept electAccept) {
    if (electAccept.isAccept()) {
      acceptorToMessageId.put(publicKey, messageID);
    }
  }

  public Set<PublicKey> getNodes() {
    return nodes;
  }

  @Override
  public String toString() {
    return String.format(
        "ElectInstance{messageId='%s', instanceId='%s', channel='%s', proposer='%s', elect=%s, nodes=%s, state=%s}",
        messageId.getEncoded(),
        getInstanceId(),
        channel,
        proposer.getEncoded(),
        elect.toString(),
        nodes.toString(),
        state);
  }

  public static String generateConsensusId(
      @NonNull String type, @NonNull String id, @NonNull String property) {
    return Hash.hash("consensus", type, id, property);
  }

  public enum State {
    FAILED,
    WAITING,
    STARTING,
    ACCEPTED
  }
}
