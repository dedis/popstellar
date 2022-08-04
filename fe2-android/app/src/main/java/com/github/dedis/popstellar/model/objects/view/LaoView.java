package com.github.dedis.popstellar.model.objects.view;

import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.*;
import java.util.stream.Collectors;

public final class LaoView {

  private final Lao lao;

  public LaoView(Lao lao) {
    if (lao == null) {
      throw new IllegalArgumentException();
    }
    this.lao = new Lao(lao);
  }

  /**
   * This should only be used by the update method of the LAORepository
   *
   * @return the wrapped LAO
   */
  public Lao getLao() {
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

  public void updateWitnessMessage(MessageID prevId, WitnessMessage witnessMessage) {
    lao.updateWitnessMessage(prevId, witnessMessage);
  }

  public void addPendingUpdate(PendingUpdate pendingUpdate) {
    lao.getPendingUpdates().add(new PendingUpdate(pendingUpdate));
  }

  public void removePendingUpdate(long targetTime) {
    lao.getPendingUpdates()
        .removeIf(pendingUpdate -> pendingUpdate.getModificationTime() <= targetTime);
  }

  public void updateLaoState(StateLao stateLao) {
    lao.setId(stateLao.getId());
    lao.setWitnesses(stateLao.getWitnesses());
    lao.setName(stateLao.getName());
    lao.setLastModified(stateLao.getLastModified());
    lao.setModificationId(stateLao.getModificationId());
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

  public void updateRollCall(String id, RollCall rollCall) {
    lao.updateRollCall(id, rollCall);
  }

  public Optional<RollCall> getRollCall(String id) {
    return lao.getRollCall(id);
  }

  public void updateTransactionHashMap(List<PublicKey> attendees) {
    lao.updateTransactionHashMap(attendees);
  }

  public void updateChirps(MessageID messageID, Chirp chirp) {
    lao.updateAllChirps(messageID, new Chirp(chirp));
  }

  public Optional<Chirp> getChirp(MessageID messageID) {
    Optional<Chirp> optional = lao.getChirp(messageID);
    return optional.map(Chirp::new); // If optional is empty returns empty
    // otherwise returns a copy of the Chirp
  }

  public Set<PublicKey> getWitnesses() {
    return lao.getWitnesses().stream().map(PublicKey::new).collect(Collectors.toSet());
  }

  public PublicKey getOrganizer() {
    return new PublicKey(lao.getOrganizer());
  }

  public void updateElectInstance(ElectInstance electInstance) {
    lao.updateElectInstance(electInstance);
  }

  public Optional<ElectInstance> getElectInstance(MessageID messageId) {
    return lao.getElectInstance(messageId);
  }

  public void updateElection(String id, Election election) {
    lao.updateElection(id, new Election(election));
  }
}
