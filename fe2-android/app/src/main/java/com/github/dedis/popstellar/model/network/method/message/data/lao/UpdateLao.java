package com.github.dedis.popstellar.model.network.method.message.data.lao;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.annotations.SerializedName;

import java.util.*;

/** Data sent to update the lao specifications */
@Immutable
public class UpdateLao extends Data {

  private final String id;
  private final String name;

  @SerializedName("last_modified")
  private final long lastModified;

  private final Set<PublicKey> witnesses;

  /**
   * Constructor for a data Update LAO
   *
   * @param organizer public key of the LAO
   * @param creation creation time
   * @param name name of the LAO
   * @param lastModified time of last modification
   * @param witnesses list of witnesses of the LAO
   */
  public UpdateLao(
      PublicKey organizer,
      long creation,
      String name,
      long lastModified,
      Set<PublicKey> witnesses) {
    this.id = Lao.generateLaoId(organizer, creation, name);
    this.name = name;
    this.lastModified = lastModified;
    this.witnesses = new HashSet<>(witnesses);
  }

  @Override
  public String getObject() {
    return Objects.LAO.getObject();
  }

  @Override
  public String getAction() {
    return Action.UPDATE.getAction();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getLastModified() {
    return lastModified;
  }

  public Set<PublicKey> getWitnesses() {
    return new HashSet<>(witnesses);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateLao updateLao = (UpdateLao) o;
    return getLastModified() == updateLao.getLastModified()
        && java.util.Objects.equals(getName(), updateLao.getName())
        && java.util.Objects.equals(getId(), updateLao.getId())
        && java.util.Objects.equals(getWitnesses(), updateLao.getWitnesses());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getName(), getLastModified(), getWitnesses());
  }

  @NonNull
  @Override
  public String toString() {
    return "UpdateLao{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", lastModified="
        + lastModified
        + ", witnesses="
        + Arrays.toString(witnesses.toArray())
        + '}';
  }
}
