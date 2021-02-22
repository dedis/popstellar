package com.github.dedis.student20_pop.model.network.method;

import com.github.dedis.student20_pop.utility.protocol.MessageHandler;

/** Query to catchup on missed messages */
public final class Catchup extends Query {

  public Catchup(String channel, int id) {
    super(channel, id);
  }

  @Override
  public void accept(MessageHandler handler) {
    handler.handle(this);
  }

  @Override
  public String getMethod() {
    return Method.CATCHUP.getMethod();
  }
}
