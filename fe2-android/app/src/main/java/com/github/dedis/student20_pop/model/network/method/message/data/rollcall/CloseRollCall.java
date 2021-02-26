package com.github.dedis.student20_pop.model.network.method.message.data.rollcall;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import java.util.List;

/** Data sent to close a Roll-Call */
public class CloseRollCall extends Data {

  private final String updateId;
  private final String closes;
  private final long end;
  private final List<String> attendees;

  /**
   * Constructor for a data Close Roll-Call Event
   *
   * @param updateId SHA256('R'||lao_id||closes||end)
   * @param closes The 'update_id' of the latest roll call open, or in its absence, the 'id' field
   *     of the roll call creation
   * @param end timestamp of the roll call end time
   * @param attendees list of attendees of the Roll-Call
   */
  public CloseRollCall(String updateId, String closes, long end, List<String> attendees) {
    this.updateId = updateId;
    this.closes = closes;
    this.end = end;
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

  public long getEnd() {
    return end;
  }

  public List<String> getAttendees() {
    return attendees;
  }
}
