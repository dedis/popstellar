package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;
import java.util.*;

/** Class modeling a Local Autonomous Organization (LAO) */
public final class Lao implements Copyable<Lao> {

  public static final String TAG = Lao.class.getSimpleName();

  private final Channel channel;
  private String id;
  private String name;
  private Long lastModified;
  private Long creation;
  private PublicKey organizer;
  private MessageID modificationId;

  private Set<PendingUpdate> pendingUpdates;

  public Lao(String id) {
    if (id == null) {
      throw new IllegalArgumentException(" The id is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException(" The id of the Lao is empty");
    }

    channel = Channel.getLaoChannel(id);
    this.id = id;
    pendingUpdates = new HashSet<>();
  }

  public Lao(String name, PublicKey organizer, long creation) {
    // This will throw an exception if name is null or empty
    this(generateLaoId(organizer, creation, name));
    this.name = name;
    this.organizer = organizer;
    this.creation = creation;
  }

  public Lao(
      Channel channel,
      String id,
      String name,
      Long lastModified,
      Long creation,
      PublicKey organizer,
      MessageID modificationId,
      Set<PendingUpdate> pendingUpdates) {
    this.channel = channel;
    this.id = id;
    this.name = name;
    this.lastModified = lastModified;
    this.creation = creation;
    this.organizer = organizer;
    this.modificationId = modificationId;
    this.pendingUpdates = new HashSet<>(pendingUpdates);
  }

  /**
   * Copy constructor
   *
   * @param lao the lao to be deep copied in a new object
   */
  public Lao(Lao lao) {
    channel = lao.channel;
    id = lao.id;
    name = lao.name;
    lastModified = lao.lastModified;
    creation = lao.creation;
    organizer = lao.organizer;
    modificationId = lao.modificationId;
    pendingUpdates = new HashSet<>(lao.pendingUpdates);
  }

  public Long getLastModified() {
    return lastModified;
  }

  public Set<PendingUpdate> getPendingUpdates() {
    return pendingUpdates;
  }

  public PublicKey getOrganizer() {
    return organizer;
  }

  public Channel getChannel() {
    return channel;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("The Id of the Lao is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException("The id of the Lao is empty");
    }

    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException(" The name of the Lao is null");
    } else if (name.isEmpty()) {
      throw new IllegalArgumentException(" The name of the Lao is empty");
    }

    this.name = name;
  }

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  public Long getCreation() {
    return creation;
  }

  public void setCreation(Long creation) {
    this.creation = creation;
  }

  public void setOrganizer(PublicKey organizer) {
    this.organizer = organizer;
  }

  public MessageID getModificationId() {
    return modificationId;
  }

  public void setModificationId(MessageID modificationId) {
    this.modificationId = modificationId;
  }

  public void addPendingUpdate(PendingUpdate pendingUpdate) {
    pendingUpdates.add(pendingUpdate);
  }

  public void setPendingUpdates(Set<PendingUpdate> pendingUpdates) {
    this.pendingUpdates = pendingUpdates;
  }

  /**
   * Generate the id for dataCreateLao and dataUpdateLao.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateLao.json
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataUpdateLao.json
   *
   * @param organizer ID of the organizer
   * @param creation creation time of the LAO
   * @param name original or updated name of the LAO
   * @return the ID of CreateLao or UpdateLao computed as Hash(organizer||creation||name)
   */
  public static String generateLaoId(PublicKey organizer, long creation, String name) {
    return Hash.hash(organizer.getEncoded(), Long.toString(creation), name);
  }

  @Override
  public Lao copy() {
    return new Lao(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Lao lao = (Lao) o;
    return Objects.equals(channel, lao.channel)
        && Objects.equals(id, lao.id)
        && Objects.equals(name, lao.name)
        && Objects.equals(lastModified, lao.lastModified)
        && Objects.equals(creation, lao.creation)
        && Objects.equals(organizer, lao.organizer)
        && Objects.equals(modificationId, lao.modificationId)
        && Objects.equals(pendingUpdates, lao.pendingUpdates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        channel, id, name, lastModified, creation, organizer, modificationId, pendingUpdates);
  }

  @NonNull
  @Override
  public String toString() {
    return "Lao{"
        + "name='"
        + name
        + '\''
        + ", id='"
        + id
        + '\''
        + ", channel='"
        + channel
        + '\''
        + ", creation="
        + creation
        + ", organizer='"
        + organizer
        + '\''
        + ", lastModified="
        + lastModified
        + ", modificationId='"
        + modificationId
        + "'}";
  }
}
