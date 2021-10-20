package com.github.dedis.popstellar.model.network.method;

/** Query to catchup on missed messages */
public final class Catchup extends Query {

  public Catchup(String channel, int id) {
    super(channel, id);
  }

  @Override
  public String getMethod() {
    return Method.CATCHUP.getMethod();
  }
}
