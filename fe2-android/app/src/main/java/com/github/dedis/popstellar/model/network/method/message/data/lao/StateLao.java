package com.github.dedis.popstellar.model.network.method.message.data.lao;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.annotations.SerializedName;

import java.util.*;

/** Data received to track the state of a lao */
public class StateLao extends Data {

  private final String id;
  private final String name;
  private final long creation;

  @SerializedName("last_modified")
  private final long lastModified;

  private final PublicKey organizer;

  @SerializedName("modification_id")
  private final MessageID modificationId;

  private final Set<PublicKey> witnesses;

  @SerializedName("modification_signatures")
  private final List<PublicKeySignaturePair> modificationSignatures;

  /**
   * Constructor for a data State LAO
   *
   * @param id of the LAO state message, Hash(organizer||creation||name)
   * @param name name of the LAO
   * @param creation time of creation
   * @param lastModified time of last modification
   * @param organizer id of the LAO's organizer
   * @param witnesses list of witnesses of the LAO
   * @throws IllegalArgumentException if the id is not valid
   */
  @Immutable
  public StateLao(
      String id,
      String name,
      long creation,
      long lastModified,
      PublicKey organizer,
      MessageID modificationId,
      Set<PublicKey> witnesses,
      List<PublicKeySignaturePair> modificationSignatures) {
    if (!id.equals(Lao.generateLaoId(organizer, creation, name))) {
      throw new IllegalArgumentException("StateLao id must be Hash(organizer||creation||name)");
    }
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.lastModified = lastModified;
    this.organizer = organizer;
    this.modificationId = modificationId;
    this.witnesses = new HashSet<>(witnesses);

    this.modificationSignatures = new ArrayList<>();
    if (modificationSignatures != null) {
      this.modificationSignatures.addAll(modificationSignatures);
    }
  }

  @Override
  public String getObject() {
    return Objects.LAO.getObject();
  }

  @Override
  public String getAction() {
    return Action.STATE.getAction();
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

  public PublicKey getOrganizer() {
    return organizer;
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

  @NonNull
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
        + ", lastModified="
        + lastModified
        + ", organizer='"
        + organizer
        + '\''
        + ", modificationId='"
        + modificationId
        + '\''
        + ", witnesses="
        + Arrays.toString(witnesses.toArray())
        + ", modificationSignatures="
        + Arrays.toString(modificationSignatures.toArray())
        + '}';
  }

  public MessageID getModificationId() {
    return modificationId;
  }

  public List<PublicKeySignaturePair> getModificationSignatures() {
    return new ArrayList<>(modificationSignatures);
  }
}
