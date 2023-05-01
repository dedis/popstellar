package com.github.dedis.popstellar.model.network.method.message.data.meeting;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.utility.MessageValidator;

import java.util.Optional;

import javax.annotation.Nullable;

/** Data sent to create a new meeting */
@Immutable
public class CreateMeeting extends Data {

  private final String id;
  private final String name;
  private final long creation;
  @Nullable private final String location;
  private final long start;
  private final long end;

  /**
   * Constructor for a data Create Meeting Event
   *
   * @param laoId id of the LAO
   * @param id of the Meeting creation message, Hash("M"||laoId||creation||name)
   * @param name name of the Meeting
   * @param creation time of creation
   * @param location location of the Meeting
   * @param start of the Meeting
   * @param end of the Meeting
   * @throws IllegalArgumentException if the id is invalid
   */
  public CreateMeeting(
      String laoId,
      String id,
      String name,
      long creation,
      @Nullable String location,
      long start,
      long end) {
    MessageValidator.verify()
        .checkBase64(laoId, "lao id")
        .checkValidCreateMeetingId(id, laoId, creation, name);

    this.id = id;
    this.name = name;
    this.creation = creation;
    this.location = location;
    this.start = start;
    if (end != 0) {
      this.end = end;
    } else {
      this.end = start + 60 * 60;
    }
  }

  public CreateMeeting(
      String laoId, String name, long creation, @Nullable String location, long start, long end) {
    id = Meeting.generateCreateMeetingId(laoId, creation, name);
    this.name = name;
    this.creation = creation;
    this.location = location;
    this.start = start;
    this.end = end;
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

  public Optional<String> getLocation() {
    return Optional.ofNullable(location);
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  @Override
  public String getObject() {
    return Objects.MEETING.getObject();
  }

  @Override
  public String getAction() {
    return Action.CREATE.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateMeeting that = (CreateMeeting) o;
    return getCreation() == that.getCreation()
        && getStart() == that.getStart()
        && getEnd() == that.getEnd()
        && java.util.Objects.equals(getId(), that.getId())
        && java.util.Objects.equals(getName(), that.getName())
        && java.util.Objects.equals(getLocation(), that.getLocation());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(), getName(), getCreation(), getLocation(), getStart(), getEnd());
  }

  @Override
  public String toString() {
    return "CreateMeeting{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", creation="
        + creation
        + ", location='"
        + location
        + '\''
        + ", start="
        + start
        + ", end="
        + end
        + '}';
  }
}
