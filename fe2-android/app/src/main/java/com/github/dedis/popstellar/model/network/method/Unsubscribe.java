package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Channel;

/** Unsubscribe from a channel */
@Immutable
public final class Unsubscribe extends Query {

  public Unsubscribe(Channel channel, int id) {
    super(channel, id);
  }

  @Override
  public String getMethod() {
    return Method.UNSUBSCRIBE.getMethod();
  }
}
