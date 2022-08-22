package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Channel;

/** Subscribe to a channel */
@Immutable
public final class Subscribe extends Query {

  public Subscribe(Channel channel, int id) {
    super(channel, id);
  }

  @Override
  public String getMethod() {
    return Method.SUBSCRIBE.getMethod();
  }
}
