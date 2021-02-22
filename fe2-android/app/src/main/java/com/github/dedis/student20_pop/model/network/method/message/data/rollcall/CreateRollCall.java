package com.github.dedis.student20_pop.model.network.method.message.data.rollcall;

import androidx.annotation.Nullable;
import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.utility.protocol.DataHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Data sent to create a Roll-Call */
public class CreateRollCall extends Data {

  private final String id;
  private final String name;
  private final long creation;
  private final transient long start;
  private final transient StartType startType;
  private final String location;

  @Nullable private final transient String description;

  /**
   * Constructor for a data Create Roll-Call Event
   *
   * @param id of the Roll-Call creation message, Hash("R"||laoId||creation||name)
   * @param name name of the Roll-Call
   * @param creation time of creation
   * @param start of the Roll-Call
   * @param startType of the Roll-Call, either scheduled or now
   * @param location location of the Roll-Call
   * @param description can be null
   */
  public CreateRollCall(
      String id,
      String name,
      long creation,
      long start,
      StartType startType,
      String location,
      @Nullable String description) {
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.start = start;
    this.startType = startType;
    this.location = location;
    this.description = description;
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

  public long getStartTime() {
    return start;
  }

  public StartType getStartType() {
    return startType;
  }

  public String getLocation() {
    return location;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  @Override
  public void accept(DataHandler handler, URI host, String channel) {
    handler.handle(this, host, channel);
  }

  @Override
  public String getObject() {
    return Objects.ROLL_CALL.getObject();
  }

  @Override
  public String getAction() {
    return Action.CREATE.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CreateRollCall that = (CreateRollCall) o;
    return getCreation() == that.getCreation()
        && start == that.start
        && java.util.Objects.equals(getId(), that.getId())
        && java.util.Objects.equals(getName(), that.getName())
        && getStartType() == that.getStartType()
        && java.util.Objects.equals(getLocation(), that.getLocation())
        && java.util.Objects.equals(getDescription(), that.getDescription());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(),
        getName(),
        getCreation(),
        getStartTime(),
        getStartType(),
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
        + ", start="
        + start
        + ", startType="
        + startType
        + ", location='"
        + location
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }

  /** Enumeration of the different starting types of a roll call */
  public enum StartType {
    NOW("start"),
    SCHEDULED("scheduled");

    public static final List<StartType> ALL =
        Collections.unmodifiableList(Arrays.asList(StartType.values()));

    // Name of the time json member for that type
    private final String jsonType;

    StartType(String jsonType) {
      this.jsonType = jsonType;
    }

    public String getJsonMember() {
      return jsonType;
    }
  }
}
