package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/** Data sent to create a Roll-Call */
@Immutable
public class CreateRollCall extends Data {

  private final String id;
  private final String name;
  private final long creation;

  @SerializedName("proposed_start")
  private final long proposedStart;

  @SerializedName("proposed_end")
  private final long proposedEnd;

  private final String location;

  @Nullable private final String description;

  /**
   * Constructor for a data Create Roll-Call Event
   *
   * @param name name of the Roll-Call
   * @param creation of the Roll-Call
   * @param proposedStart of the Roll-Call
   * @param proposedEnd of the Roll-Call
   * @param location location of the Roll-Call
   * @param description can be null
   * @param laoId ID of the LAO
   */
  public CreateRollCall(
      String name,
      long creation,
      long proposedStart,
      long proposedEnd,
      String location,
      @Nullable String description,
      String laoId) {
    this.name = name;
    this.creation = creation;
    this.proposedStart = proposedStart;
    if (proposedEnd == 0) {
      this.proposedEnd = proposedStart + 3600;
    } else {
      this.proposedEnd = proposedEnd;
    }
    this.location = location;
    this.description = description;
    id = RollCall.generateCreateRollCallId(laoId, creation, name);
  }

  public CreateRollCall(
      String id,
      String name,
      long creation,
      long proposedStart,
      long proposedEnd,
      String location,
      @Nullable String description) {
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.proposedStart = proposedStart;
    this.proposedEnd = proposedEnd;
    this.location = location;
    this.description = description;
  }

  @Override
  public String getObject() {
    return Objects.ROLL_CALL.getObject();
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

  public long getProposedStart() {
    return proposedStart;
  }

  public long getProposedEnd() {
    return proposedEnd;
  }

  public String getLocation() {
    return location;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateRollCall that = (CreateRollCall) o;
    return getCreation() == that.getCreation()
        && proposedStart == that.proposedStart
        && proposedEnd == that.proposedEnd
        && java.util.Objects.equals(getId(), that.getId())
        && java.util.Objects.equals(getName(), that.getName())
        && java.util.Objects.equals(getLocation(), that.getLocation())
        && java.util.Objects.equals(getDescription(), that.getDescription());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(),
        getName(),
        getCreation(),
        getProposedStart(),
        getProposedEnd(),
        getLocation(),
        getDescription());
  }

  @Override
  public String toString() {
    return "CreateRollCall{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", creation="
        + creation
        + ", proposedStart="
        + proposedStart
        + ", proposedEnd="
        + proposedEnd
        + ", location='"
        + location
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
