package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Channel;

/** Query to catchup on missed messages */
@Immutable
public final class Catchup extends Query {

  public Catchup(Channel channel, int id) {
    super(channel, id);
  }

  @Override
  public String getMethod() {
    return Method.CATCHUP.getMethod();
  }
}
