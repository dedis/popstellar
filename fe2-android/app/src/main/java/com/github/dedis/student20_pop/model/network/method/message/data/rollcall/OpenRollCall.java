package com.github.dedis.student20_pop.model.network.method.message.data.rollcall;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.github.dedis.student20_pop.utility.protocol.DataHandler;
import java.net.URI;

/** Data sent to open a roll call */
public class OpenRollCall extends Data {

  private final String id;
  private final long start;

  /**
   * Constructor of a data Open Roll-Call
   *
   * @param id of the open Roll-Call message, Hash("R"||laoId||creation||name)
   * @param start of the Roll-Call
   */
  public OpenRollCall(String id, long start) {
    this.id = id;
    this.start = start;
  }

  public String getId() {
    return id;
  }

  public long getStart() {
    return start;
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
    return Action.OPEN.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OpenRollCall that = (OpenRollCall) o;
    return getStart() == that.getStart() && java.util.Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getId(), getStart());
  }

  @Override
  public String toString() {
    return "OpenRollCall{" + "id='" + id + '\'' + ", start=" + start + '}';
  }
}
