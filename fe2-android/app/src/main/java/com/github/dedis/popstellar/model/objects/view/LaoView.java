package com.github.dedis.popstellar.model.objects.view;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.util.*;

public final class LaoView {

  private final Lao lao;

  /**
   * This class offers useful getters for Lao state to handlers and prevents changing its state. It
   * is provided as an intermediate step towards functional handling of Objects. To change the state
   * of an Lao, one can use createLaoCopy() which returns a copy of the wrapped Lao, and update the
   * repository with said updated LAO.
   *
   * @param lao the Lao to be wrapped
   */
  public LaoView(Lao lao) {
    if (lao == null) {
      throw new IllegalArgumentException();
    }
    this.lao = new Lao(lao);
  }

  public Lao createLaoCopy() {
    return new Lao(lao);
  }

  public long getLastModified() {
    return lao.getLastModified();
  }

  public String getName() {
    return lao.getName();
  }

  public String getId() {
    return lao.getId();
  }

  public boolean areWitnessSetsEqual(Set<PublicKey> witnesses) {
    return lao.getWitnesses().equals(witnesses);
  }

  public boolean isWitnessesEmpty() {
    return lao.getWitnesses().isEmpty();
  }

  public boolean isOrganizer(PublicKey publicKey) {
    return lao.getOrganizer().equals(publicKey);
  }

  public boolean isWitness(PublicKey publicKey) {
    return lao.getWitnesses().contains(publicKey);
  }

  public Channel getChannel() {
    return lao.getChannel();
  }

  public Optional<RollCall> getRollCall(String id) {
    Optional<RollCall> optional = lao.getRollCall(id);
    return optional.map(RollCall::new); // If empty returns empty optional, if not
    // returns optional with copy of retrieved RollCall
  }

  public Optional<RollCall> getRollCallWithPersistentId(String persistentId) {
    Optional<RollCall> optional = lao.getRollCallWithPersistentId(persistentId);
    return optional.map(RollCall::new); // If empty returns empty optional, if not
    // returns optional with copy of retrieved RollCall
  }

  public Optional<Election> getElection(String id) {
    Optional<Election> electionOption = lao.getElection(id);
    return electionOption.map(Election::new); // If empty returns empty optional, if not
    // returns optional with copy of retrieved Election
  }

  public Set<PublicKey> getWitnesses() {
    return new HashSet<>(lao.getWitnesses());
  }

  public PublicKey getOrganizer() {
    return lao.getOrganizer();
  }

  public Optional<ElectInstance> getElectInstance(MessageID messageId) {
    // TODO uncomment that when consensus does not rely on call by reference
    //    Optional<ElectInstance> optional = lao.getElectInstance(messageId);
    //    return optional.map(ElectInstance::new); // If empty returns empty optional, if not
    // returns optional with copy of retrieved ElectInstance
    return lao.getElectInstance(messageId);
  }

  public RollCall getMostRecentRollCall() throws NoRollCallException {
    return new RollCall(lao.lastRollCallClosed());
  }

  public Map<String, RollCall> getRollCalls() {
    return Copyable.copy(lao.getRollCalls());
  }

  public Map<String, Election> getElections() {
    return Copyable.copy(lao.getElections());
  }

  public Map<MessageID, WitnessMessage> getWitnessMessages() {
    return Copyable.copy(lao.getWitnessMessages());
  }

  public List<ConsensusNode> getNodes() {
    return lao.getNodes();
  }

  public ConsensusNode getNode(@NonNull PublicKey key) {
    return lao.getNode(key);
  }

  public long getCreation() {
    return lao.getCreation();
  }

  public Map<PublicKey, List<TransactionObject>> getTransactionHistoryByUser() {
    return Copyable.copyMapOfList(lao.getTransactionHistoryByUser());
  }

  public Map<PublicKey, List<TransactionObject>> getTransactionByUser() {
    return Copyable.copyMapOfList(lao.getTransactionByUser());
  }

  @NonNull
  @Override
  public String toString() {
    return lao.toString();
  }
}
