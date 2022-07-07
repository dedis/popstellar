package com.github.dedis.popstellar.model.network.method;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.Channel;

import org.junit.Test;

public class SubscribeTest {

  @Test
  public void getMethodTest() {
    Subscribe subscribe = new Subscribe(Channel.fromString("root/stuff"), 2);
    assertEquals("subscribe", subscribe.getMethod());
  }
}
