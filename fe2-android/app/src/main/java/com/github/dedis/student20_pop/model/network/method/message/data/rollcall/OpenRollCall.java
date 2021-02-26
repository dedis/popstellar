package com.github.dedis.student20_pop.model.network.method.message.data.rollcall;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;

/** Data sent to open a roll call */
public class OpenRollCall extends Data {

  private final String updateId;
  private final String opens;
  private final long start;

  /**
   * Constructor of a data Open Roll-Call
   *
   * @param updateId id of this message Hash("R"||laoId||opens||start)
   * @param opens The 'update_id' of the latest roll call close, or in its absence, the 'id' field
   *     of the roll call creation
   * @param start timestamp corresponding to roll call open
   */
  public OpenRollCall(String updateId, String opens, long start) {
    this.updateId = updateId;
    this.opens = opens;
    this.start = start;
  }

  public long getStart() {
    return start;
  }

  @Override
  public String getObject() {
    return Objects.ROLL_CALL.getObject();
  }

  @Override
  public String getAction() {
    return Action.OPEN.getAction();
  }

  public String getUpdateId() {
    return updateId;
  }

  public String getOpens() {
    return opens;
  }
}
