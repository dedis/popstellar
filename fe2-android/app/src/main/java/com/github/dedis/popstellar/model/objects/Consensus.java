package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Consensus {

  private String messageId;
  private String channel;
  private String id;

  private ConsensusKey key;
  private Object value;

  private long creation;

  private boolean isAccepted;
  private boolean isFailed;

  private String proposer;
  private Set<String> nodes;
  // map the public key of acceptors to the id of their message
  private final Map<String, String> acceptorToMessageId;

  public Consensus(long creation, ConsensusKey key, Object value) {
    this.id = generateConsensusId(key.getType(), key.getId(), key.getProperty());
    this.key = key;
    this.value = value;
    this.creation = creation;

    this.isAccepted = false;
    this.acceptorToMessageId = new HashMap<>();
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(@NonNull String messageId) {
    this.messageId = messageId;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(@NonNull String channel) {
    this.channel = channel;
  }

  public String getId() {
    return id;
  }

  public void setId(@NonNull String id) {
    this.id = id;
  }

  public ConsensusKey getKey() {
    return key;
  }

  public void setKey(@NonNull ConsensusKey key) {
    this.key = key;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public long getCreation() {
    return creation;
  }

  public void setCreation(long creation) {
    if (creation < 0) {
      throw new IllegalArgumentException("creation time cannot be negative");
    }
    this.creation = creation;
  }

  public String getProposer() {
    return proposer;
  }

  public void setProposer(@NonNull String proposer) {
    this.proposer = proposer;
  }

  public Set<String> getNodes() {
    return nodes;
  }

  public void setNodes(@NonNull Set<String> nodes) {
    this.nodes = nodes;
  }

  public Map<String, String> getAcceptorsToMessageId() {
    return Collections.unmodifiableMap(acceptorToMessageId);
  }

  public void putPositiveAcceptorResponse(@NonNull String acceptor, @NonNull String messageId) {
    acceptorToMessageId.put(acceptor, messageId);
  }

  public boolean isAccepted() {
    return isAccepted;
  }

  public void setAccepted(boolean accepted) {
    isAccepted = accepted;
  }

  public boolean isFailed() {
    return isFailed;
  }

  public void setFailed(boolean failed) {
    this.isFailed = failed;
  }

  public boolean canBeAccepted() {
    // Part 1 : all acceptors need to accept
    int countAccepted = acceptorToMessageId.size();
    return countAccepted == nodes.size();
  }

  @Override
  public String toString() {
    return String.format(
        "Consensus{id='%s', channel='%s', messageId='%s', key=%s, value='%s', creation=%s, isAccepted=%b, isFailed=%b, proposer='%s'}",
        id, channel, messageId, key, value, creation, isAccepted, isFailed, proposer);
  }

  public static String generateConsensusId(String type, String id, String property) {
    return Hash.hash("consensus", type, id, property);
  }
}
