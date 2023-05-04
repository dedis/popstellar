package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.google.gson.annotations.SerializedName;

/** Data sent to open a roll call */
@Immutable
public class OpenRollCall extends Data {

  @SerializedName("update_id")
  private final String updateId;

  private final String opens;

  @SerializedName("opened_at")
  private final long openedAt;

  private final String action;

  /**
   * Constructor of a data Open Roll-Call
   *
   * @param laoId id of lao
   * @param opens The 'update_id' of the latest roll call close, or in its absence, the 'id' field
   *     of the roll call creation
   * @param openedAt timestamp corresponding to roll call open
   * @param state the state in which the roll call is when this instance is created
   */
  public OpenRollCall(String laoId, String opens, long openedAt, EventState state) {
    updateId = RollCall.generateOpenRollCallId(laoId, opens, openedAt);
    this.opens = opens;
    this.openedAt = openedAt;
    if (state == EventState.CLOSED) {
      action = Action.REOPEN.getAction();
    } else {
      action = Action.OPEN.getAction();
    }
  }

  public OpenRollCall(String updateId, String opens, long openedAt, String action) {
    this.updateId = updateId;
    this.opens = opens;
    this.openedAt = openedAt;
    this.action = action;
  }

  @Override
  public String getObject() {
    return Objects.ROLL_CALL.getObject();
  }

  @Override
  public String getAction() {
    return action;
  }

  public String getUpdateId() {
    return updateId;
  }

  public String getOpens() {
    return opens;
  }

  public long getOpenedAt() {
    return openedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenRollCall that = (OpenRollCall) o;

    return openedAt == that.openedAt
        && java.util.Objects.equals(updateId, that.updateId)
        && java.util.Objects.equals(opens, that.opens)
        && java.util.Objects.equals(action, that.action);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(updateId, opens, openedAt, action);
  }

  @Override
  public String toString() {
    return "OpenRollCall{"
        + "updateId='"
        + updateId
        + '\''
        + ", opens='"
        + opens
        + '\''
        + ", openedAt="
        + openedAt
        + ", action='"
        + action
        + '\''
        + '}';
  }
}
