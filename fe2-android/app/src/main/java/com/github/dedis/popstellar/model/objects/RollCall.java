package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.github.dedis.popstellar.model.Copyable;
import com.github.dedis.popstellar.model.objects.event.*;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.util.*;

/**
 * Class representing a roll call object It is immutable in practice. However in a few tests in
 * RollCallFragmentTest, state is mutably changed because of legacy code in LaoDetailViewModel (the
 * currentRollCall setting to be precise) (Johann Plüss : 15.12.2022)
 */
public class RollCall extends Event implements Copyable<RollCall> {

  private final String id;
  private final String persistentId;
  private final String name;
  private final long creation;
  private final long start;
  private final long end;
  private EventState state;
  private final Set<PublicKey> attendees;

  private final String location;
  private final String description;

  public RollCall(
      String id,
      String persistentId,
      String name,
      long creation,
      long start,
      long end,
      EventState state,
      Set<PublicKey> attendees,
      String location,
      String description) {
    this.id = id;
    this.persistentId = persistentId;
    this.name = name;
    this.creation = creation;
    this.start = start;
    this.end = end;
    this.state = state;
    this.attendees = attendees;
    this.location = location;
    this.description = description;
  }

  public RollCall(RollCall rollCall) {
    this.id = rollCall.id;
    this.persistentId = rollCall.persistentId;
    this.name = rollCall.name;
    this.creation = rollCall.creation;
    this.start = rollCall.start;
    this.end = rollCall.end;
    this.state = rollCall.state;
    this.attendees = new HashSet<>(rollCall.attendees);
    this.location = rollCall.location;
    this.description = rollCall.description;
  }

  public String getId() {
    return id;
  }

  public String getPersistentId() {
    return persistentId;
  }

  public String getName() {
    return name;
  }

  public long getCreation() {
    return creation;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  @VisibleForTesting
  public void setState(EventState state) {
    this.state = state;
  }

  public Set<PublicKey> getAttendees() {
    return attendees;
  }

  public String getLocation() {
    return location;
  }

  public String getDescription() {
    return description;
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
  public EventState getState() {
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

  public static RollCall openRollCall(RollCall rollCall) {
    return setRollCallState(rollCall, EventState.OPENED);
  }

  public static RollCall closeRollCall(RollCall rollCall) {
    return setRollCallState(rollCall, EventState.CLOSED);
  }

  private static RollCall setRollCallState(RollCall rollCall, EventState state) {
    RollCall updatedRollCall = new RollCall(rollCall);
    updatedRollCall.state = state;
    return updatedRollCall;
  }

  /**
   * @return true if the roll-call is closed, false otherwise
   */
  public boolean isClosed() {
    return EventState.CLOSED.equals(state);
  }

  @Override
  public RollCall copy() {
    return new RollCall(this);
  }

  @NonNull
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
        + state
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
