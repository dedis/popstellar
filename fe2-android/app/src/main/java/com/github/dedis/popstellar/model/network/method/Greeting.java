package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;

public class Greeting extends Message{

  /**
   * Constructor for a Message
   *
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  protected Greeting(Channel channel) {
    super(channel);
  }

  @Override
  public String getMethod() {
    return null;
  }
}
