package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

/** Data sent to close a Roll-Call */
public class CloseRollCall extends Data {

  @SerializedName("update_id")
  private final String updateId;

  private final String closes;

  @SerializedName("closed_at")
  private final long closedAt;

  private final List<String> attendees;

  /**
   * Constructor for a data Close Roll-Call Event
   *
   * @param laoId id of the LAO
   * @param closes The 'update_id' of the latest roll call open, or in its absence, the 'id' field
   *     of the roll call creation
   * @param closedAt timestamp of the roll call close
   * @param attendees list of attendees of the Roll-Call
   */
  public CloseRollCall(String laoId, String closes, long closedAt, List<String> attendees) {
    this.updateId = RollCall.generateCloseRollCallId(laoId, closes, closedAt);
    this.closes = closes;
    this.closedAt = closedAt;
    this.attendees = attendees;
  }

  @Override
  public String getObject() {
    return Objects.ROLL_CALL.getObject();
  }

  @Override
  public String getAction() {
    return Action.CLOSE.getAction();
  }

  public String getUpdateId() {
    return updateId;
  }

  public String getCloses() {
    return closes;
  }

  public long getClosedAt() {
    return closedAt;
  }

  public List<String> getAttendees() {
    return attendees;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloseRollCall that = (CloseRollCall) o;

    return closedAt == that.closedAt
        && java.util.Objects.equals(updateId, that.updateId)
        && java.util.Objects.equals(closes, that.closes)
        && java.util.Objects.equals(attendees, that.attendees);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(updateId, closes, closedAt, attendees);
  }

  @Override
  public String toString() {
    return "CloseRollCall{"
        + "updateId='"
        + updateId
        + '\''
        + ", closes='"
        + closes
        + '\''
        + ", closedAt="
        + closedAt
        + ", attendees="
        + Arrays.toString(attendees.toArray())
        + '}';
  }
}
