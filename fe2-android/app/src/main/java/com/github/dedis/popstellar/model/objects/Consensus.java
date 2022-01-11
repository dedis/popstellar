package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Consensus {

  private MessageID messageId;
  private String channel;
  private String id;

  private ConsensusKey key;
  private Object value;

  private long creation;

  private boolean isAccepted;
  private boolean isFailed;

  private PublicKey proposer;
  private Set<PublicKey> nodes;
  // map the public key of acceptors to the id of their message
  private final Map<PublicKey, MessageID> acceptorToMessageId;

  public Consensus(long creation, ConsensusKey key, Object value) {
    this.id = generateConsensusId(key.getType(), key.getId(), key.getProperty());
    this.key = key;
    this.value = value;
    this.creation = creation;

    this.isAccepted = false;
    this.acceptorToMessageId = new HashMap<>();
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public void setMessageId(@NonNull MessageID messageId) {
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

  public PublicKey getProposer() {
    return proposer;
  }

  public void setProposer(@NonNull PublicKey proposer) {
    this.proposer = proposer;
  }

  public Set<PublicKey> getNodes() {
    return nodes;
  }

  public void setNodes(@NonNull Set<PublicKey> nodes) {
    this.nodes = nodes;
  }

  public Map<PublicKey, MessageID> getAcceptorsToMessageId() {
    return Collections.unmodifiableMap(acceptorToMessageId);
  }

  public void putPositiveAcceptorResponse(
      @NonNull PublicKey acceptor, @NonNull MessageID messageId) {
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
