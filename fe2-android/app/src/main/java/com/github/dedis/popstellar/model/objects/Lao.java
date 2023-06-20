package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.*;

/** Class modeling a Local Autonomous Organization (LAO) */
public final class Lao implements Copyable<Lao> {

  public static final String TAG = Lao.class.getSimpleName();

  private final Channel channel;
  private String id;
  private String name;
  private Long lastModified;
  private Long creation;
  private PublicKey organizer;
  private MessageID modificationId;

  private Set<PendingUpdate> pendingUpdates;

  private final Map<MessageID, ElectInstance> messageIdToElectInstance;
  private final Map<PublicKey, ConsensusNode> keyToNode;

  public Lao(String id) {
    if (id == null) {
      throw new IllegalArgumentException(" The id is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException(" The id of the Lao is empty");
    }

    channel = Channel.getLaoChannel(id);
    this.id = id;
    keyToNode = new HashMap<>();
    messageIdToElectInstance = new HashMap<>();
    pendingUpdates = new HashSet<>();
  }

  public Lao(String name, PublicKey organizer, long creation) {
    // This will throw an exception if name is null or empty
    this(generateLaoId(organizer, creation, name));
    this.name = name;
    this.organizer = organizer;
    this.creation = creation;
  }

  public Lao(
      Channel channel,
      String id,
      String name,
      Long lastModified,
      Long creation,
      PublicKey organizer,
      MessageID modificationId,
      Set<PendingUpdate> pendingUpdates,
      Map<MessageID, ElectInstance> messageIdToElectInstance,
      Map<PublicKey, ConsensusNode> keyToNode) {
    this.channel = channel;
    this.id = id;
    this.name = name;
    this.lastModified = lastModified;
    this.creation = creation;
    this.organizer = organizer;
    this.modificationId = modificationId;
    this.pendingUpdates = new HashSet<>(pendingUpdates);
    this.messageIdToElectInstance = new HashMap<>(messageIdToElectInstance);
    this.keyToNode = Copyable.copy(keyToNode);
  }

  /**
   * Copy constructor
   *
   * @param lao the lao to be deep copied in a new object
   */
  public Lao(Lao lao) {
    channel = lao.channel;
    id = lao.id;
    name = lao.name;
    lastModified = lao.lastModified;
    creation = lao.creation;
    organizer = lao.organizer;
    modificationId = lao.modificationId;
    pendingUpdates = new HashSet<>(lao.pendingUpdates);
    // FIXME We need to keep the ElectInstance because the current consensus relies on references
    // (Gabriel Fleischer 11.08.22)
    messageIdToElectInstance = new HashMap<>(lao.messageIdToElectInstance);
    keyToNode = Copyable.copy(lao.keyToNode);
  }

  /**
   * Store the given ElectInstance and update all nodes concerned by it.
   *
   * @param electInstance the ElectInstance
   */
  public void updateElectInstance(@NonNull ElectInstance electInstance) {
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

  public Optional<ElectInstance> getElectInstance(MessageID messageId) {
    return Optional.ofNullable(messageIdToElectInstance.get(messageId));
  }

  public Long getLastModified() {
    return lastModified;
  }

  public Set<PendingUpdate> getPendingUpdates() {
    return pendingUpdates;
  }

  public PublicKey getOrganizer() {
    return organizer;
  }

  public Channel getChannel() {
    return channel;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("The Id of the Lao is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException("The id of the Lao is empty");
    }

    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException(" The name of the Lao is null");
    } else if (name.isEmpty()) {
      throw new IllegalArgumentException(" The name of the Lao is empty");
    }

    this.name = name;
  }

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  public Long getCreation() {
    return creation;
  }

  public void setCreation(Long creation) {
    this.creation = creation;
  }

  public void setOrganizer(PublicKey organizer) {
    this.organizer = organizer;
    keyToNode.computeIfAbsent(organizer, ConsensusNode::new);
  }

  public MessageID getModificationId() {
    return modificationId;
  }

  public void setModificationId(MessageID modificationId) {
    this.modificationId = modificationId;
  }

  public void initKeyToNode(Set<PublicKey> witnesses) {
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

  public void addPendingUpdate(PendingUpdate pendingUpdate) {
    pendingUpdates.add(pendingUpdate);
  }

  public void setPendingUpdates(Set<PendingUpdate> pendingUpdates) {
    this.pendingUpdates = pendingUpdates;
  }

  /**
   * Get the list of all nodes of this Lao sorted by the base64 representation of their public key.
   *
   * @return a sorted List of ConsensusNode
   */
  public List<ConsensusNode> getNodes() {
    List<ConsensusNode> nodes = new ArrayList<>(keyToNode.values());
    nodes.sort(Comparator.comparing(node -> node.getPublicKey().getEncoded()));
    return nodes;
  }

  public ConsensusNode getNode(@NonNull PublicKey key) {
    return keyToNode.get(key);
  }

  public Map<MessageID, ElectInstance> getMessageIdToElectInstance() {
    return Collections.unmodifiableMap(messageIdToElectInstance);
  }

  public Map<PublicKey, ConsensusNode> getKeyToNode() {
    return keyToNode;
  }

  /**
   * Generate the id for dataCreateLao and dataUpdateLao.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateLao.json
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataUpdateLao.json
   *
   * @param organizer ID of the organizer
   * @param creation creation time of the LAO
   * @param name original or updated name of the LAO
   * @return the ID of CreateLao or UpdateLao computed as Hash(organizer||creation||name)
   */
  public static String generateLaoId(PublicKey organizer, long creation, String name) {
    return Hash.hash(organizer.getEncoded(), Long.toString(creation), name);
  }

  @Override
  public Lao copy() {
    return new Lao(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Lao lao = (Lao) o;
    return Objects.equals(channel, lao.channel)
        && Objects.equals(id, lao.id)
        && Objects.equals(name, lao.name)
        && Objects.equals(lastModified, lao.lastModified)
        && Objects.equals(creation, lao.creation)
        && Objects.equals(organizer, lao.organizer)
        && Objects.equals(modificationId, lao.modificationId)
        && Objects.equals(pendingUpdates, lao.pendingUpdates)
        && Objects.equals(messageIdToElectInstance, lao.messageIdToElectInstance)
        && Objects.equals(keyToNode, lao.keyToNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        channel,
        id,
        name,
        lastModified,
        creation,
        organizer,
        modificationId,
        pendingUpdates,
        messageIdToElectInstance,
        keyToNode);
  }

  @NonNull
  @Override
  public String toString() {
    return "Lao{"
        + "name='"
        + name
        + '\''
        + ", id='"
        + id
        + '\''
        + ", channel='"
        + channel
        + '\''
        + ", creation="
        + creation
        + ", organizer='"
        + organizer
        + '\''
        + ", lastModified="
        + lastModified
        + ", modificationId='"
        + modificationId
        + '\''
        + ", electInstances="
        + messageIdToElectInstance.values()
        + ", transactionPerUser="
        + '}';
  }
}
