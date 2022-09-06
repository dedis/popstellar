package com.github.dedis.popstellar.model.network.method.message.data.lao;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.time.Instant;
import java.util.*;

/** Data sent when creating a new LAO */
@Immutable
public class CreateLao extends Data {

  private final String id;
  private final String name;
  private final long creation;
  private final PublicKey organizer;
  private final List<PublicKey> witnesses;

  /**
   * Constructor for a data Create LAO
   *
   * @param id of the LAO creation message, Hash(organizer||creation||name)
   * @param name name of the LAO
   * @param creation time of creation
   * @param organizer id of the LAO's organizer
   * @param witnesses list of witnesses of the LAO
   * @throws IllegalArgumentException if the id is not valid
   */
  public CreateLao(
      String id, String name, long creation, PublicKey organizer, List<PublicKey> witnesses) {
    if (!id.equals(Lao.generateLaoId(organizer, creation, name))) {
      throw new IllegalArgumentException("CreateLao id must be Hash(organizer||creation||name)");
    }
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.organizer = organizer;
    this.witnesses = new ArrayList<>(witnesses);
  }

  public CreateLao(String name, PublicKey organizer) {
    this.name = name;
    this.organizer = organizer;
    this.creation = Instant.now().getEpochSecond();
    this.id = Lao.generateLaoId(organizer, creation, name);
    this.witnesses = new ArrayList<>();
  }

  @Override
  public String getObject() {
    return Objects.LAO.getObject();
  }

  @Override
  public String getAction() {
    return Action.CREATE.getAction();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getCreation() {
    return creation;
  }

  public PublicKey getOrganizer() {
    return organizer;
  }

  public List<PublicKey> getWitnesses() {
    return new ArrayList<>(witnesses);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateLao createLao = (CreateLao) o;
    return getCreation() == createLao.getCreation()
        && java.util.Objects.equals(getId(), createLao.getId())
        && java.util.Objects.equals(getName(), createLao.getName())
        && java.util.Objects.equals(getOrganizer(), createLao.getOrganizer())
        && java.util.Objects.equals(getWitnesses(), createLao.getWitnesses());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(), getName(), getCreation(), getOrganizer(), getWitnesses());
  }

  @NonNull
  @Override
  public String toString() {
    return "CreateLao{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", creation="
        + creation
        + ", organizer='"
        + organizer
        + '\''
        + ", witnesses="
        + Arrays.toString(witnesses.toArray())
        + '}';
  }
}
