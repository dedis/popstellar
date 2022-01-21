package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Class modeling a Local Autonomous Organization (LAO) */
public final class Lao {

  private String channel;
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
  private List<MessageID> chirpsIdInOrder;
  private Map<MessageID, Chirp> allChirps;
  private Map<PublicKey, List<MessageID>> chirpsByUser;
  private final Map<MessageID, Consensus> messageIdToConsensus;
  private final List<ConsensusNode> nodes;

  public Lao(String id) {
    if (id == null) {
      throw new IllegalArgumentException(" The id is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException(" The id of the Lao is empty");
    }
    this.channel = "/root/" + id;
    this.id = id;
    this.rollCalls = new HashMap<>();
    this.elections = new HashMap<>();
    this.chirpsIdInOrder = new ArrayList<>();
    this.allChirps = new HashMap<>();
    this.chirpsByUser = new HashMap<>();
    this.nodes = new ArrayList<>();
    this.messageIdToConsensus = new HashMap<>();
    this.witnessMessages = new HashMap<>();
    this.witnesses = new HashSet<>();
    this.pendingUpdates = new HashSet<>();
  }

  public Lao(String name, PublicKey organizer, long creation) {
    this(generateLaoId(organizer, creation, name));
    if (name == null) {
      throw new IllegalArgumentException("The name of the Lao is null");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("The name of the Lao is empty");
    }
    this.name = name;
    this.organizer = organizer;
    this.creation = creation;
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
   * Store the given consensus and update all nodes concerned by it.
   *
   * @param consensus the consensus
   */
  public void updateConsensus(Consensus consensus) {
    if (consensus == null) {
      throw new IllegalArgumentException("The consensus is null");
    }
    messageIdToConsensus.put(consensus.getMessageId(), consensus);

    Map<PublicKey, MessageID> acceptorsToMessageId = consensus.getAcceptorsToMessageId();
    // add to each node the messageId of the consensus if they accept it
    nodes.stream()
        .filter(node -> acceptorsToMessageId.containsKey(node.getPublicKey()))
        .forEach(node -> node.addMessageIdOfAnAcceptedConsensus(consensus.getMessageId()));

    // add the consensus to node if it is proposer
    nodes.stream()
        .filter(node -> node.getPublicKey().equals(consensus.getProposer()))
        .forEach(node -> node.addConsensus(consensus));
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
   * Update the list of chirps that have been set in the lao in descending order of timestamp. If
   * the list of chirps contain one * with Id prevId, it will remove it from the list then add the
   * new chirp into it.
   *
   * @param prevId the previous id of a chirp
   * @param index in which we should insert the chirp id
   */
  public void updateChirpsInOrder(MessageID prevId, int index) {
    chirpsIdInOrder.remove(prevId);
    chirpsIdInOrder.add(index, prevId);
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

    int index1 = findIndexOfChirpIdOrderedByTime(chirpsIdInOrder, allChirps, chirp.getTimestamp());
    updateChirpsInOrder(prevId, index1);

    int index2 = 0;
    PublicKey user = chirp.getSender();
    if (chirpsByUser.containsKey(user)) {
      index2 =
          findIndexOfChirpIdOrderedByTime(chirpsByUser.get(user), allChirps, chirp.getTimestamp());
    }
    updateChirpsByUser(prevId, user, index2);
  }

  /**
   * Update the list of chirps that have been set in the lao from a user. If the list of chirps
   * contain one * with Id prevId, it will remove it from the list then add the new chirp into it.
   *
   * @param prevId the previous id of a chirp
   * @param user the user that sent the chirp
   */
  public void updateChirpsByUser(MessageID prevId, PublicKey user, int index) {
    if (chirpsByUser.containsKey(user)) {
      chirpsByUser.get(user).add(index, prevId);
    } else {
      chirpsByUser.put(user, new ArrayList<>());
      chirpsByUser.get(user).add(prevId);
    }
  }

  /**
   * Helper function to find the index of where we should insert the chirp
   *
   * @param chirpsId list of chirps in descending order of timestamp
   * @param allChirps map of MessageID to Chirp containing all chirps sent in the lao
   * @param timestamp of the chirp to insert
   * @return index in the list chirpsId
   */
  public int findIndexOfChirpIdOrderedByTime(
      List<MessageID> chirpsId, Map<MessageID, Chirp> allChirps, long timestamp) {
    int left = 0;
    int right = chirpsId.size();

    while (left < right) {
      final int mid = (left + right) / 2;
      if (allChirps.get(chirpsId.get(mid)).getTimestamp() > timestamp) {
        left = mid + 1;
      } else {
        right = mid;
      }
    }
    return left;
  }

  public Optional<RollCall> getRollCall(String id) {
    return Optional.ofNullable(rollCalls.get(id));
  }

  public Optional<Election> getElection(String id) {
    return Optional.ofNullable(elections.get(id));
  }

  public Optional<Consensus> getConsensus(MessageID messageId) {
    return Optional.ofNullable(messageIdToConsensus.get(messageId));
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

  public boolean removeConsensus(MessageID messageId) {
    return (messageIdToConsensus.remove(messageId) != null);
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

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
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
    if (nodes.stream().map(ConsensusNode::getPublicKey).noneMatch(organizer::equals)) {
      nodes.add(new ConsensusNode(organizer));
    }
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
    witnesses.forEach(
        w -> {
          if (nodes.stream().noneMatch(node -> node.getPublicKey().equals(w))) {
            nodes.add(new ConsensusNode(w));
          }
        });
  }

  public void setPendingUpdates(Set<PendingUpdate> pendingUpdates) {
    this.pendingUpdates = pendingUpdates;
  }

  public List<ConsensusNode> getNodes() {
    return nodes;
  }

  public Map<String, Election> getElections() {
    return elections;
  }

  public Map<String, RollCall> getRollCalls() {
    return rollCalls;
  }

  public Map<MessageID, Consensus> getMessageIdToConsensus() {
    return messageIdToConsensus;
  }

  public Map<MessageID, WitnessMessage> getWitnessMessages() {
    return witnessMessages;
  }

  public List<MessageID> getChirpsIdInOrder() {
    return chirpsIdInOrder;
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
   * Generate the id for dataCreateLao and dataUpdateLao.
   * https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataCreateLao.json
   * https://github.com/dedis/student_21_pop/blob/master/protocol/query/method/message/data/dataUpdateLao.json
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
        + ", consensuses="
        + messageIdToConsensus.values()
        + '}';
  }
}
