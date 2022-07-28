package com.github.dedis.popstellar.model.objects;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.*;
import java.util.stream.Collectors;

/** Class modeling a Local Autonomous Organization (LAO) */
public final class Lao {

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

  private Map<String, RollCall> rollCalls;
  private Map<String, Election> elections;
  private Map<MessageID, Chirp> allChirps;
  private Map<PublicKey, List<MessageID>> chirpsByUser;
  private final Map<MessageID, ElectInstance> messageIdToElectInstance;
  private final Map<PublicKey, ConsensusNode> keyToNode;
  // Some useful map for the digital cash
  private Map<String, PublicKey> pubKeyByHash;
  // Map for the history
  private Map<PublicKey, List<TransactionObject>> transactionHistoryByUser;
  // Map for the the public_key last transaction
  private Map<PublicKey, List<TransactionObject>> transactionByUser;

  public Lao(String id) {
    if (id == null) {
      throw new IllegalArgumentException(" The id is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException(" The id of the Lao is empty");
    }

    this.channel = Channel.getLaoChannel(id);
    this.id = id;
    this.rollCalls = new HashMap<>();
    this.elections = new HashMap<>();
    this.allChirps = new HashMap<>();
    this.chirpsByUser = new HashMap<>();
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
    this.channel = new Channel(lao.channel);
    this.id = lao.id;
    this.name = lao.name;
    this.lastModified = lao.lastModified;
    this.creation = lao.creation;
    this.organizer = new PublicKey(lao.organizer);
    this.modificationId = new MessageID(lao.modificationId);
    this.witnesses = lao.witnesses.stream().map(PublicKey::new).collect(Collectors.toSet());
    this.witnessMessages =
        lao.witnessMessages.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new MessageID(entry.getKey()),
                    entry -> new WitnessMessage(entry.getValue())));
    this.pendingUpdates =
        lao.pendingUpdates.stream().map(PendingUpdate::new).collect(Collectors.toSet());
    this.rollCalls =
        lao.rollCalls.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> new RollCall(entry.getValue())));
    this.elections =
        lao.elections.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> new Election(entry.getValue())));
    this.allChirps =
        lao.allChirps.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new MessageID(entry.getKey()), entry -> new Chirp(entry.getValue())));
    this.chirpsByUser =
        lao.chirpsByUser.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new PublicKey(entry.getKey()),
                    entry ->
                        entry.getValue().stream()
                            .map(MessageID::new)
                            .collect(Collectors.toList())));
    this.messageIdToElectInstance =
        lao.messageIdToElectInstance.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new MessageID(entry.getKey()),
                    entry -> new ElectInstance(entry.getValue())));
    this.keyToNode =
        lao.keyToNode.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new PublicKey(entry.getKey()),
                    entry -> new ConsensusNode(entry.getValue())));
    this.pubKeyByHash =
        lao.pubKeyByHash.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> new PublicKey(entry.getValue())));
    this.transactionHistoryByUser =
        lao.transactionHistoryByUser.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new PublicKey(entry.getKey()),
                    entry ->
                        entry.getValue().stream()
                            .map(TransactionObject::new)
                            .collect(Collectors.toList())));
    this.transactionByUser =
        lao.transactionByUser.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> new PublicKey(entry.getKey()),
                    entry ->
                        entry.getValue().stream()
                            .map(TransactionObject::new)
                            .collect(Collectors.toList())));
  }

  public void updateRollCall(String prevId, RollCall rollCall) {
    if (rollCall == null) {
      throw new IllegalArgumentException("The roll call is null");
    }

    rollCalls.remove(prevId);
    rollCalls.put(rollCall.getId(), rollCall);
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

  /**
   * Update the list of chirps that have been sent in the lao. If the list of chirps contain one
   * with Id prevId, it will remove it from the list then add the new chirp into it.
   *
   * @param prevId the previous id of a chirp
   * @param chirp the chirp
   */
  public void updateAllChirps(MessageID prevId, Chirp chirp) {
    if (chirp == null) {
      throw new IllegalArgumentException("The chirp is null");
    }
    allChirps.remove(prevId);
    allChirps.put(chirp.getId(), chirp);

    PublicKey user = chirp.getSender();
    chirpsByUser.computeIfAbsent(user, key -> new ArrayList<>()).add(prevId);
  }

  /**
   * Function which update the transaction map public key by transaction hash on the list of the
   * roll call attendees Update pubKeyByHash, Initialize transactionByUser, transactionHistoryByUser
   *
   * @param attendees List<PublicKey> of the roll call attendees
   */
  public void updateTransactionHashMap(List<PublicKey> attendees) {
    pubKeyByHash = new HashMap<>();
    pubKeyByHash.put(organizer.computeHash(), organizer);
    attendees.forEach(publicKey -> pubKeyByHash.put(publicKey.computeHash(), publicKey));

    // also update the history and the current transaction per attendees
    // both map have to be set to empty again
    transactionByUser = new HashMap<>();
    transactionHistoryByUser = new HashMap<>();
  }

  /**
   * Function that update all the transaction Update transactionByUser (current state of money)
   * Update transactionHistory (current transaction perform per user)
   *
   * @param transactionObject object which was posted and now should update the lao map
   */
  public void updateTransactionMaps(TransactionObject transactionObject) {
    if (transactionObject == null) {
      throw new IllegalArgumentException("The transaction is null");
    }
    /* Change the transaction per public key in transacionperUser
    for the sender and the receiver*/
    if (this.getRollCalls().values().isEmpty()) {
      throw new IllegalStateException("A transaction need a roll call creation ");
    }
    if (this.pubKeyByHash.isEmpty()) {
      throw new IllegalStateException("A transaction need attendees !");
    }

    /* Contained in the receiver there are also the sender
    which has to be in the list of attendees of the roll call*/
    for (PublicKey current : transactionObject.getReceiversTransaction(pubKeyByHash)) {
      // Add the transaction in the current state  / for the sender and the receiver

      /* The only case where the map has a list of transaction in memory is when we have several
      coin base transaction (in fact the issuer send several time money to someone)
      or our receiver is no sender */
      if (transactionByUser.containsKey(current)
          && (transactionObject.isCoinBaseTransaction()
              || (transactionObject.isReceiver(current) && !transactionObject.isSender(current)))) {
        transactionHistoryByUser.putIfAbsent(current, new ArrayList<>());
        List<TransactionObject> list = new ArrayList<>(transactionByUser.get(current));
        list.add(transactionObject);
        transactionByUser.replace(current, list);
      } else {
        transactionByUser.put(current, Collections.singletonList(transactionObject));
      }

      // Add the transaction in the history / for the sender and the receiver
      transactionHistoryByUser.putIfAbsent(current, new ArrayList<>());
      if (!transactionHistoryByUser.get(current).add(transactionObject)) {
        throw new IllegalStateException("Problem occur by updating the transaction history");
      }
    }
    Log.d(TAG, "Transaction by history : " + transactionHistoryByUser.toString());
    Log.d(this.getClass().toString(), "Transaction by User : " + transactionByUser.toString());
  }

  public Optional<RollCall> getRollCall(String id) {
    return Optional.ofNullable(rollCalls.get(id));
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

  public Optional<Chirp> getChirp(MessageID id) {
    return Optional.ofNullable(allChirps.get(id));
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

  /**
   * Removes a roll call from the list of roll calls.
   *
   * @param id the id of the Roll Call
   * @return true if the roll call was deleted
   */
  public boolean removeRollCall(String id) {
    return (rollCalls.remove(id) != null);
  }

  public boolean removeElectInstance(MessageID messageId) {
    return (messageIdToElectInstance.remove(messageId) != null);
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

  public Map<String, RollCall> getRollCalls() {
    return rollCalls;
  }

  public Map<MessageID, ElectInstance> getMessageIdToElectInstance() {
    return Collections.unmodifiableMap(messageIdToElectInstance);
  }

  public Map<MessageID, WitnessMessage> getWitnessMessages() {
    return witnessMessages;
  }

  public List<Chirp> getChirpsInOrder() {
    return allChirps.values().stream()
        .sorted(Comparator.comparingLong(Chirp::getTimestamp).reversed())
        .collect(Collectors.toList());
  }

  public Map<PublicKey, List<TransactionObject>> getTransactionHistoryByUser() {
    return transactionHistoryByUser;
  }

  public Map<PublicKey, List<TransactionObject>> getTransactionByUser() {
    return transactionByUser;
  }

  public Map<String, PublicKey> getPubKeyByHash() {
    return pubKeyByHash;
  }

  public Map<MessageID, Chirp> getAllChirps() {
    return allChirps;
  }

  public void setRollCalls(Map<String, RollCall> rollCalls) {
    this.rollCalls = rollCalls;
  }

  public void setElections(Map<String, Election> elections) {
    this.elections = elections;
  }

  /**
   * Class which return the last roll call open
   *
   * @return Rollcall the roll call with the last ending tim e
   */
  public RollCall lastRollCallClosed() throws NoRollCallException {
    return this.getRollCalls().values().stream()
        .filter(RollCall::isClosed)
        .max(Comparator.comparing(RollCall::getEnd))
        .orElseThrow(() -> new NoRollCallException(this));
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
        + ", rollCalls="
        + rollCalls
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
