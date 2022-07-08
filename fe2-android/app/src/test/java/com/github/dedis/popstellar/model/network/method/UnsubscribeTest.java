package com.github.dedis.popstellar.model.network.method;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.Channel;

import org.junit.Test;

public class UnsubscribeTest {

  @Test
  public void getMethod() {
    Unsubscribe unsubscribe = new Unsubscribe(Channel.fromString("root/stuff"), 3);
    assertEquals("unsubscribe", unsubscribe.getMethod());
  }
}
