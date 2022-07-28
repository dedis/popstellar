package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.ElectInstance.State;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.*;
import java.util.stream.Collectors;

/** Class representing a Node for consensus. */
public final class ConsensusNode {

  private final PublicKey publicKey;
  // contains messageId of Elect that this node has accepted
  private final Set<MessageID> acceptedMessageIds;
  // list of all elect instances that this node has created
  private final Set<ElectInstance> electInstances;

  public ConsensusNode(PublicKey publicKey) {
    this.publicKey = publicKey;
    this.electInstances = new HashSet<>();
    this.acceptedMessageIds = new HashSet<>();
  }

  public ConsensusNode(ConsensusNode consensusNode) {
    this.publicKey = consensusNode.publicKey;
    this.acceptedMessageIds =
        consensusNode.acceptedMessageIds.stream().map(MessageID::new).collect(Collectors.toSet());
    this.electInstances =
        consensusNode.electInstances.stream().map(ElectInstance::new).collect(Collectors.toSet());
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Get a set of messageId of Elect that this node has accepted.
   *
   * @return the set of accepted Elect messageId
   */
  public Set<MessageID> getAcceptedMessageIds() {
    return Collections.unmodifiableSet(acceptedMessageIds);
  }

  /**
   * Get the latest ElectInstance that this node has created for the given instanceId.
   *
   * @param instanceId the id of the consensus
   * @return an Optional ElectInstance
   */
  public Optional<ElectInstance> getLastElectInstance(String instanceId) {
    // get the latest ElectInstance for the given instanceId
    return electInstances.stream()
        .filter(elect -> elect.getInstanceId().equals(instanceId))
        .max(Comparator.comparingLong(ElectInstance::getCreation));
  }

  /**
   * Get the state of this node for the latest ElectInstance that this node has created for the
   * given instanceId. If empty, it will return WAITING.
   *
   * @param instanceId the id of the consensus
   * @return the current state
   */
  public State getState(String instanceId) {
    Optional<ElectInstance> lastElect = getLastElectInstance(instanceId);
    return lastElect.map(ElectInstance::getState).orElse(State.WAITING);
  }

  /**
   * Add the given ElectInstance to this node. If it was already present, do nothing.
   *
   * @param electInstance the ElectInstance to add
   */
  public void addElectInstance(ElectInstance electInstance) {
    MessageID messageId = electInstance.getMessageId();
    // if the ElectInstance is not present, add it
    if (electInstances.stream().map(ElectInstance::getMessageId).noneMatch(messageId::equals)) {
      electInstances.add(electInstance);
    }
  }

  /**
   * Add the given messageId to the list of accepted Elect.
   *
   * @param electMessageId the messageId of the Elect to add
   */
  public void addMessageIdOfAnAcceptedElect(MessageID electMessageId) {
    this.acceptedMessageIds.add(electMessageId);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "ConsensusNode{publicKey='%s', acceptedMessageIds='%s', electInstances='%s'}",
        publicKey.getEncoded(), acceptedMessageIds, electInstances.toString());
  }
}
