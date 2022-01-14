package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.ElectInstance.State;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public Set<MessageID> getAcceptedMessageIds() {
    return Collections.unmodifiableSet(acceptedMessageIds);
  }

  public Optional<ElectInstance> getLastElectInstance(String instanceId) {
    // get the latest ElectInstance for the given instanceId
    return electInstances.stream()
        .filter(elect -> elect.getInstanceId().equals(instanceId))
        .max(Comparator.comparingLong(ElectInstance::getCreation));
  }

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
    electInstances.add(electInstance);
  }

  /**
   * Add the given messageId to the list of accepted Elect.
   *
   * @param electMessageId the messageId of the Elect to add
   */
  public void addMessageIdOfAnAcceptedElect(MessageID electMessageId) {
    this.acceptedMessageIds.add(electMessageId);
  }

  @Override
  public String toString() {
    return String.format(
        "ConsensusNode{publicKey='%s', acceptedMessageIds='%s', electInstances='%s'}",
        publicKey.getEncoded(), acceptedMessageIds, electInstances.toString());
  }
}
