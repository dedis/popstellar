package com.github.dedis.popstellar.model.objects.view;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Optional;
import java.util.Set;
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
    return lao.getRollCall(id);
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

  public Optional<ElectInstance> getElectInstance(MessageID messageId) {
    return lao.getElectInstance(messageId);
  }
}
