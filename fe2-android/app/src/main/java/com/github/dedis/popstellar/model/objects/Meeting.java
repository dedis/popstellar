package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.event.*;
import com.github.dedis.popstellar.utility.security.Hash;

@Immutable
public class Meeting extends Event {
  private final String id;
  private final String name;
  private final long creation;
  private final long start;
  private final long end;
  private final String location;
  private final long lastModified;

  // TODO: to implement when adding the StateMeeting functionality
  /*
  private final String modificationId = null;
  private final List<String> modificationSignatures = null;
  */

  public Meeting(
      String id,
      String name,
      long creation,
      long start,
      long end,
      String location,
      long lastModified) {
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.start = start;
    this.end = end;
    this.location = location;
    this.lastModified = lastModified;
  }

  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  public long getCreation() {
    return creation;
  }

  public String getLocation() {
    return location;
  }

  public long getLastModified() {
    return lastModified;
  }

  @Override
  public long getStartTimestamp() {
    return start;
  }

  @Override
  public EventType getType() {
    return EventType.MEETING;
  }

  @Override
  public long getEndTimestamp() {
    if (end == 0) {
      return Long.MAX_VALUE;
    }
    return end;
  }

  @Override
  public EventState getState() {
    // The meeting is considered closed when the end time is in the past
    // This info is just used to display the correct text in the event list
    if (isEndPassed()) {
      return EventState.CLOSED;
    }
    return EventState.CREATED;
  }

  /**
   * Generate the id for dataCreateMeeting. <a
   * href="https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateMeeting.json">Ref</a>
   *
   * @param laoId ID of the LAO
   * @param creation creation time of Meeting
   * @param name name of Meeting
   * @return the ID of CreateMeeting computed as Hash('M'||lao_id||creation||name)
   */
  public static String generateCreateMeetingId(String laoId, long creation, String name) {
    return Hash.hash(EventType.MEETING.getSuffix(), laoId, Long.toString(creation), name);
  }

  /**
   * Generate the id for dataStateMeeting. <a
   * href="https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataStateMeeting.json">Ref</a>
   *
   * @param laoId ID of the LAO
   * @param creation creation time of Meeting
   * @param name name of Meeting
   * @return the ID of StateMeeting computed as Hash('M'||lao_id||creation||name)
   */
  public static String generateStateMeetingId(String laoId, long creation, String name) {
    return Hash.hash(EventType.MEETING.getSuffix(), laoId, Long.toString(creation), name);
  }

  @NonNull
  @Override
  public String toString() {
    return "Meeting{"
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
        + ", end="
        + end
        + ", location='"
        + location
        + '\''
        + ", lastModified="
        + lastModified
        + '}';
  }
}
