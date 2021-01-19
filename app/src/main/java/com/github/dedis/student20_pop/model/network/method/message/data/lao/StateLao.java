package com.github.dedis.student20_pop.model.network.method.message.data.lao;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.utility.protocol.DataHandler;

import java.util.ArrayList;
import java.util.List;

/** Data received to track the state of a lao */
public class StateLao extends Data {

  private final String id;
  private final String name;
  private final long creation;
  private final long lastModified;
  private final String organizer;
  private final List<String> witnesses;

  /**
   * Constructor for a data State LAO
   *
   * @param id of the LAO state message, Hash(organizer||creation||name)
   * @param name name of the LAO
   * @param creation time of creation
   * @param lastModified time of last modification
   * @param organizer id of the LAO's organizer
   * @param witnesses list of witnesses of the LAO
   */
  public StateLao(
      String id,
      String name,
      long creation,
      long lastModified,
      String organizer,
      List<String> witnesses) {
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.lastModified = lastModified;
    this.organizer = organizer;
    this.witnesses = witnesses;
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

  public long getLastModified() {
    return lastModified;
  }

  public String getOrganizer() {
    return organizer;
  }

  public List<String> getWitnesses() {
    return new ArrayList<>(witnesses);
  }

  @Override
  public void accept(DataHandler handler) {
    handler.handle(this);
  }

  @Override
  public String getObject() {
    return Objects.LAO.getObject();
  }

  @Override
  public String getAction() {
    return Action.STATE.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StateLao stateLao = (StateLao) o;
    return getCreation() == stateLao.getCreation()
        && getLastModified() == stateLao.getLastModified()
        && java.util.Objects.equals(getId(), stateLao.getId())
        && java.util.Objects.equals(getName(), stateLao.getName())
        && java.util.Objects.equals(getOrganizer(), stateLao.getOrganizer())
        && java.util.Objects.equals(getWitnesses(), stateLao.getWitnesses());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(), getName(), getCreation(), getLastModified(), getOrganizer(), getWitnesses());
  }

  @Override
  public String toString() {
    return "StateLao{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", creation="
        + creation
        + ", last_modified="
        + lastModified
        + ", organizer='"
        + organizer
        + '\''
        + ", witnesses="
        + witnesses
        + '}';
  }
}
