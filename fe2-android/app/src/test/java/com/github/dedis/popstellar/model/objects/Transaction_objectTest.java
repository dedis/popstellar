package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Transaction_objectTest {

  private static Transaction_object transaction_object = new Transaction_object();

  @Test
  public void setAndGetChannelTest() {
    Channel channel = Channel.fromString("/root/laoId/coin/myChannel");
    transaction_object.setChannel(channel);
    assertEquals(channel, transaction_object.getChannel());
  }
}
