package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Consensus {

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
  private final Map<String, String>
      acceptorToMessageId; // map the public key of acceptors to the id of their message

  public Consensus(long creation, ConsensusKey key, Object value) {
    this.id = generateConsensusId(creation, key.getType(), key.getId(), key.getProperty(), value);
    this.key = key;
    this.value = value;
    this.creation = creation;

    this.isAccepted = false;
    this.acceptorToMessageId = new HashMap<>();
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    if (messageId == null) {
      throw new IllegalArgumentException("consensus message id shouldn't be null");
    }
    this.messageId = messageId;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    if (channel == null) {
      throw new IllegalArgumentException("consensus channel shouldn't be null");
    }
    this.channel = channel;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("consensus id shouldn't be null");
    }
    this.id = id;
  }

  public ConsensusKey getKey() {
    return key;
  }

  public void setKey(ConsensusKey key) {
    if (key == null) {
      throw new IllegalArgumentException("consensus key shouldn't be null");
    }
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
      throw new IllegalArgumentException();
    }
    this.creation = creation;
  }

  public String getProposer() {
    return proposer;
  }

  public void setProposer(String proposer) {
    if (proposer == null) {
      throw new IllegalArgumentException("consensus proposer shouldn't be null");
    }
    this.proposer = proposer;
  }

  public Set<String> getNodes() {
    return nodes;
  }

  public void setNodes(Set<String> nodes) {
    if (nodes == null) {
      throw new IllegalArgumentException("consensus nodes shouldn't be null");
    }
    this.nodes = nodes;
  }

  public Map<String, String> getAcceptorsToMessageId() {
    return acceptorToMessageId;
  }

  public void putAcceptorResponse(String acceptor, String messageId, boolean accept) {
    if (acceptor == null) {
      throw new IllegalArgumentException("Acceptor public key cannot be null.");
    }
    if (messageId == null) {
      throw new IllegalArgumentException("Message id cannot be null.");
    }
    if (accept) {
      acceptorToMessageId.put(acceptor, messageId);
    }
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
    long countAccepted = acceptorToMessageId.size();
    return countAccepted == nodes.size();
  }

  @Override
  public String toString() {
    return String.format(
        "Consensus{id='%s', channel='%s', messageId='%s', key=%s, value='%s', creation=%s, isAccepted=%b, isFailed=%b, proposer='%s'}",
        id, channel, messageId, key, value, creation, isAccepted, isFailed, proposer);
  }

  public static String generateConsensusId(
      long createdAt, String type, String id, String property, Object value) {
    return Hash.hash(
        "consensus", Long.toString(createdAt), type, id, property, String.valueOf(value));
  }
}
