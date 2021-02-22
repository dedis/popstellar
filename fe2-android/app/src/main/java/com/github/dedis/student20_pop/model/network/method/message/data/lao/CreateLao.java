package com.github.dedis.student20_pop.model.network.method.message.data.lao;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.utility.protocol.DataHandler;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Data sent when creating a new LAO */
public class CreateLao extends Data {

  private final String id;
  private final String name;
  private final long creation;
  private final String organizer;
  private final List<String> witnesses;

  /**
   * Constructor for a data Create LAO
   *
   * @param id of the LAO creation message, Hash(organizer||creation||name)
   * @param name name of the LAO
   * @param creation time of creation
   * @param organizer id of the LAO's organizer
   * @param witnesses list of witnesses of the LAO
   */
  public CreateLao(
      String id, String name, long creation, String organizer, List<String> witnesses) {
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.organizer = organizer;
    this.witnesses = witnesses;
  }

  public CreateLao(String name, String organizer) {
    this.name = name;
    this.organizer = organizer;
    this.creation = Instant.now().toEpochMilli();
    this.id = Hash.hash("L", organizer, Long.toString(creation), name);
    this.witnesses = new ArrayList<>();
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

  public String getOrganizer() {
    return organizer;
  }

  public List<String> getWitnesses() {
    return new ArrayList<>(witnesses);
  }

  @Override
  public void accept(DataHandler handler, URI host, String channel) {
    handler.handle(this, host, channel);
  }

  @Override
  public String getObject() {
    return Objects.LAO.getObject();
  }

  @Override
  public String getAction() {
    return Action.CREATE.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
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
        + witnesses
        + '}';
  }
}
