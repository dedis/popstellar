package com.github.dedis.popstellar.model.objects;

public class ConsensusNode {

  public enum State {
    FAILED,
    WAITING,
    STARTING,
  }

  private final String publicKey;
  private State state;
  private Consensus consensus; // is null if this node has not created a consensus

  public ConsensusNode(String publicKey) {
    this.publicKey = publicKey;
    this.state = State.WAITING;
    this.consensus = null;
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
