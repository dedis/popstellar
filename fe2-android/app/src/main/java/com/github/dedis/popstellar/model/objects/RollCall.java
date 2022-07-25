package com.github.dedis.popstellar.model.objects;

import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.model.objects.event.*;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.*;

public class RollCall extends Event {

  private String id;
  private final String persistentId;
  private String name;
  private long creation;
  private long start;
  private long end;
  private final MutableLiveData<EventState> state = new MutableLiveData<>();
  private Set<PublicKey> attendees;

  private String location;
  private String description;

  public RollCall(String id) {
    this.id = id;
    this.persistentId = id;
    this.attendees = new HashSet<>();
  }

  public RollCall(String laoId, long creation, String name) {
    this(generateCreateRollCallId(laoId, creation, name));
    if (name == null) {
      throw new IllegalArgumentException("The name of the RollCall is null");
    }
    this.name = name;
    this.creation = creation;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPersistentId() {
    return persistentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getCreation() {
    return creation;
  }

  public void setCreation(long creation) {
    this.creation = creation;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public void setState(EventState state) {
    this.state.postValue(state);
  }

  public Set<PublicKey> getAttendees() {
    return attendees;
  }

  public void setAttendees(Set<PublicKey> attendees) {
    this.attendees = attendees;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public long getStartTimestamp() {
    return start;
  }

  @Override
  public EventType getType() {
    return EventType.ROLL_CALL;
  }

  @Override
  public long getEndTimestamp() {
    if (end == 0) {
      return Long.MAX_VALUE;
    }
    return end;
  }

  @Override
  public MutableLiveData<EventState> getState() {
    return state;
  }

  /**
   * Generate the id for dataCreateRollCall.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateRollCall.json
   *
   * @param laoId ID of the LAO
   * @param creation creation time of RollCall
   * @param name name of RollCall
   * @return the ID of CreateRollCall computed as Hash('R'||lao_id||creation||name)
   */
  public static String generateCreateRollCallId(String laoId, long creation, String name) {
    return Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, Long.toString(creation), name);
  }

  /**
   * Generate the id for dataOpenRollCall.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataOpenRollCall.json
   *
   * @param laoId ID of the LAO
   * @param opens id of RollCall to open
   * @param openedAt open time of RollCall
   * @return the ID of OpenRollCall computed as Hash('R'||lao_id||opens||opened_at)
   */
  public static String generateOpenRollCallId(String laoId, String opens, long openedAt) {
    return Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, opens, Long.toString(openedAt));
  }

  /**
   * Generate the id for dataCloseRollCall.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCloseRollCall.json
   *
   * @param laoId ID of the LAO
   * @param closes id of RollCall to close
   * @param closedAt closing time of RollCall
   * @return the ID of CloseRollCall computed as Hash('R'||lao_id||closes||closed_at)
   */
  public static String generateCloseRollCallId(String laoId, String closes, long closedAt) {
    return Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, closes, Long.toString(closedAt));
  }

  /**
   * @return true if the roll-call is closed, false otherwise
   */
  public boolean isClosed() {
    return EventState.CLOSED.equals(state.getValue());
  }

  @Override
  public String toString() {
    return "RollCall{"
        + "id='"
        + id
        + '\''
        + ", persistentId='"
        + persistentId
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
        + ", state="
        + state.getValue()
        + ", attendees="
        + Arrays.toString(attendees.toArray())
        + ", location='"
        + location
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
