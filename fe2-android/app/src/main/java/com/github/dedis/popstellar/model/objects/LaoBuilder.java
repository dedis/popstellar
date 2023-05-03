package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.*;

public class LaoBuilder {

  private Channel channel;
  private String id;
  private String name;
  private Long lastModified;
  private Long creation;
  private PublicKey organizer;
  private MessageID modificationId;
  private Set<PublicKey> witnesses;
  private Map<MessageID, WitnessMessage> witnessMessages;
  private Set<PendingUpdate> pendingUpdates = new HashSet<>();
  private Map<MessageID, ElectInstance> messageIdToElectInstance = new HashMap<>();
  private Map<PublicKey, ConsensusNode> keyToNode = new HashMap<>();

  public LaoBuilder() {}

  public LaoBuilder setChannel(Channel channel) {
    if (channel == null) {
      throw new IllegalStateException("Channel is null");
    }
    this.channel = channel;
    return this;
  }

  public LaoBuilder setId(String id) {
    this.id = id;
    return this;
  }

  public LaoBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public LaoBuilder setLastModified(Long lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public LaoBuilder setCreation(Long creation) {
    this.creation = creation;
    return this;
  }

  public LaoBuilder setOrganizer(PublicKey organizer) {
    if (organizer == null) {
      throw new IllegalStateException("Organizer is null");
    }
    this.organizer = organizer;
    return this;
  }

  public LaoBuilder setModificationId(MessageID modificationId) {
    this.modificationId = modificationId;
    return this;
  }

  public LaoBuilder setWitnesses(Set<PublicKey> witnesses) {
    if (witnesses == null) {
      throw new IllegalStateException("Witnesses is null");
    }
    this.witnesses = witnesses;
    return this;
  }

  public LaoBuilder setWitnessMessages(Map<MessageID, WitnessMessage> witnessMessages) {
    if (witnessMessages == null) {
      throw new IllegalStateException("WitnessMessages is null");
    }
    this.witnessMessages = witnessMessages;
    return this;
  }

  public LaoBuilder setPendingUpdates(Set<PendingUpdate> pendingUpdates) {
    if (pendingUpdates == null) {
      throw new IllegalStateException("PendingUpdates is null");
    }
    this.pendingUpdates = pendingUpdates;
    return this;
  }

  public LaoBuilder setMessageIdToElectInstance(
      Map<MessageID, ElectInstance> messageIdToElectInstance) {
    if (messageIdToElectInstance == null) {
      throw new IllegalStateException("MessageIdToElectInstance is null");
    }
    this.messageIdToElectInstance = messageIdToElectInstance;
    return this;
  }

  public LaoBuilder setKeyToNode(Map<PublicKey, ConsensusNode> keyToNode) {
    if (keyToNode == null) {
      throw new IllegalStateException("KeyToNode is null");
    }
    this.keyToNode = keyToNode;
    return this;
  }

  public Lao build() {
    if (channel == null) {
      throw new IllegalStateException("Channel is null");
    }
    if (organizer == null) {
      throw new IllegalStateException("Organizer is null");
    }
    if (witnesses == null) {
      throw new IllegalStateException("Witnesses is null");
    }
    if (witnessMessages == null) {
      throw new IllegalStateException("WitnessMessages is null");
    }
    if (pendingUpdates == null) {
      throw new IllegalStateException("PendingUpdates is null");
    }
    if (messageIdToElectInstance == null) {
      throw new IllegalStateException("MessageIdToElectInstance is null");
    }
    if (keyToNode == null) {
      throw new IllegalStateException("KeyToNode is null");
    }
    return new Lao(
        channel,
        id,
        name,
        lastModified,
        creation,
        organizer,
        modificationId,
        witnesses,
        witnessMessages,
        pendingUpdates,
        messageIdToElectInstance,
        keyToNode);
  }
}
