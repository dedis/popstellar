package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
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
  private Set<PublicKey> witnesses;
  private final Map<MessageID, WitnessMessage> witnessMessages;
  /**
   * map between a messages ID and the corresponding object WitnessMessage that has to be signed by
   * witnesses
   */
  private Set<PendingUpdate> pendingUpdates;

  private Map<String, Election> elections;
  private final Map<MessageID, ElectInstance> messageIdToElectInstance;
  private final Map<PublicKey, ConsensusNode> keyToNode;
  // Some useful map for the digital cash
  private Map<String, PublicKey> pubKeyByHash;
  // Map for the history
  private Map<PublicKey, Set<TransactionObject>> transactionHistoryByUser;
  // Map for the the public_key last transaction
  private Map<PublicKey, Set<TransactionObject>> transactionByUser;

  public Lao(String id) {
    if (id == null) {
      throw new IllegalArgumentException(" The id is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException(" The id of the Lao is empty");
    }

    this.channel = Channel.getLaoChannel(id);
    this.id = id;
    this.elections = new HashMap<>();
    this.keyToNode = new HashMap<>();
    this.messageIdToElectInstance = new HashMap<>();
    this.witnessMessages = new HashMap<>();
    this.witnesses = new HashSet<>();
    this.pendingUpdates = new HashSet<>();
    // initialize the maps :
    this.transactionHistoryByUser = new HashMap<>();
    this.transactionByUser = new HashMap<>();
    this.pubKeyByHash = new HashMap<>();
  }

  public Lao(String name, PublicKey organizer, long creation) {
    // This will throw an exception if name is null or empty
    this(generateLaoId(organizer, creation, name));
    this.name = name;
    this.organizer = organizer;
    this.creation = creation;
    pubKeyByHash.put(organizer.computeHash(), organizer);
  }

  /**
   * Copy constructor
   *
   * @param lao the lao to be deep copied in a new object
   */
  public Lao(Lao lao) {
    this.channel = lao.channel;
    this.id = lao.id;
    this.name = lao.name;
    this.lastModified = lao.lastModified;
    this.creation = lao.creation;
    this.organizer = lao.organizer;
    this.modificationId = lao.modificationId;
    this.witnesses = new HashSet<>(lao.witnesses);
    this.witnessMessages = new HashMap<>(lao.witnessMessages);
    this.pendingUpdates = new HashSet<>(lao.pendingUpdates);
    this.elections = Copyable.copy(lao.elections);
    // FIXME We need to keep the ElectInstance because the current consensus relies on references
    // (Gabriel Fleischer 11.08.22)
    this.messageIdToElectInstance = new HashMap<>(lao.messageIdToElectInstance);
    this.keyToNode = Copyable.copy(lao.keyToNode);
    this.pubKeyByHash = new HashMap<>(lao.pubKeyByHash);
    this.transactionHistoryByUser = new HashMap<>(lao.transactionHistoryByUser);
    this.transactionByUser = new HashMap<>(lao.transactionByUser);
  }

  public void updateElection(String prevId, Election election) {
    if (election == null) {
      throw new IllegalArgumentException("The election is null");
    }

    elections.remove(prevId);
    elections.put(election.getId(), election);
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

  /**
   * Update the list of messages that have to be signed by witnesses. If the list of messages
   * contain the message with Id prevId , it will remove this message from the list. Then it will
   * add the new message to the list with the corresponding newId
   *
   * @param prevId the previous id of a message that needs to be signed
   * @param witnessMessage the object representing the message needing to be signed
   */
  public void updateWitnessMessage(MessageID prevId, WitnessMessage witnessMessage) {
    witnessMessages.remove(prevId);
    witnessMessages.put(witnessMessage.getMessageId(), witnessMessage);
  }

  public Optional<Election> getElection(String id) {
    return Optional.ofNullable(elections.get(id));
  }

  public Optional<ElectInstance> getElectInstance(MessageID messageId) {
    return Optional.ofNullable(messageIdToElectInstance.get(messageId));
  }

  public Optional<WitnessMessage> getWitnessMessage(MessageID id) {
    return Optional.ofNullable(witnessMessages.get(id));
  }

  /**
   * Removes an election from the list of elections.
   *
   * @param id the id of the Election
   * @return true if the election was deleted
   */
  public boolean removeElection(String id) {
    return (elections.remove(id) != null);
  }

  public Long getLastModified() {
    return lastModified;
  }

  public Set<PublicKey> getWitnesses() {
    return witnesses;
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

  public void setWitnesses(Set<PublicKey> witnesses) {

    if (witnesses == null) {
      throw new IllegalArgumentException("The witnesses set is null");
    }
    for (PublicKey witness : witnesses) {
      if (witness == null) {
        throw new IllegalArgumentException("One of the witnesses in the set is null");
      }
    }
    this.witnesses = witnesses;
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

  public Map<String, Election> getElections() {
    return elections;
  }

  public Map<MessageID, ElectInstance> getMessageIdToElectInstance() {
    return Collections.unmodifiableMap(messageIdToElectInstance);
  }

  public Map<MessageID, WitnessMessage> getWitnessMessages() {
    return witnessMessages;
  }

  public Map<PublicKey, Set<TransactionObject>> getTransactionHistoryByUser() {
    return transactionHistoryByUser;
  }

  public Map<PublicKey, Set<TransactionObject>> getTransactionByUser() {
    return transactionByUser;
  }

  public Map<String, PublicKey> getPubKeyByHash() {
    return pubKeyByHash;
  }

  public void setElections(Map<String, Election> elections) {
    this.elections = elections;
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
        + ", witnesses="
        + witnesses
        + ", elections="
        + elections
        + ", electInstances="
        + messageIdToElectInstance.values()
        + ", transactionPerUser="
        + transactionByUser.toString()
        + ", transactionHistoryByUser"
        + transactionHistoryByUser.toString()
        + ", pubKeyByHash"
        + pubKeyByHash.toString()
        + '}';
  }
}
