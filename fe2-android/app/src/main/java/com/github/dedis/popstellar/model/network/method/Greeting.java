package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;

/**
 * Greeting message, doesn't expect any answers, sent by the backend after connecting to it
 */
public class Greeting extends Message {

  /**
   * Constructor for a Greeting Message
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  public Greeting(Channel channel) {
    super(channel);
  }

  @Override
  public String getMethod() {
    return Method.GREETING.getMethod();
  }
}
