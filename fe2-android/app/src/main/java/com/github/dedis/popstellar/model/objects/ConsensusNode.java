package com.github.dedis.popstellar.model.objects;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ConsensusNode {

  public enum State {
    FAILED,
    WAITING,
    STARTING,
    ACCEPTED
  }

  private final String publicKey;
  private State state;
  private Consensus consensus; // is null if this node has not created a consensus
  private List<Consensus> consensuses;
  private final Set<String> acceptedMessageIds;

  public Optional<Consensus> getLastConsensus(String keyId) {
    // get the consensus for the given key id, with the largest creation time
    return consensuses.stream()
        .filter(consensus -> consensus.getKey().getId().equals(keyId))
        .max((c1, c2) -> (int) (c1.getCreation() - c2.getCreation()));
  }

  public State getState(String keyId) {
    Optional<Consensus> lastConsensus = getLastConsensus(keyId);
    if (lastConsensus.isPresent()) {
      Consensus consensus = lastConsensus.get();
      if (consensus.isFailed()) {
        return State.FAILED;
      } else if (consensus.isAccepted()) {
        return State.ACCEPTED;
      } else {
        return State.STARTING;
      }
    } else {
      return State.WAITING;
    }
  }

  public Set<String> getAcceptedMessageIds() {
    return acceptedMessageIds;
  }

  public void addConsensus(Consensus consensus) {
    if (consensuses.stream().noneMatch(c2 -> c2.getMessageId().equals(consensus.getMessageId()))) {
      consensuses.add(consensus);
    }
  }

  public void addAcceptedMessageIds(String messageId) {
    this.acceptedMessageIds.add(messageId);
  }

  public ConsensusNode(String publicKey) {
    this.publicKey = publicKey;
    this.state = State.WAITING;
    this.consensus = null;
    this.consensuses = Collections.emptyList();
    this.acceptedMessageIds = new HashSet<>();
  }

  public String getPublicKey() {
    return publicKey;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public Consensus getConsensus() {
    return consensus;
  }

  public void setConsensus(Consensus consensus) {
    this.consensus = consensus;
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusNode{publicKey='%s', state='%s', consensus='%s'}", publicKey, state, consensus);
  }
}
