package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class AddChirpBroadcastText {

  private static final String postId = "postId";
  private static final String channel = "/root/laoId/social/myChannel";
  private static final long timestamp = 1631280815;

  private static final AddChirpBroadcast addChirpBroadcast =
      new AddChirpBroadcast(postId, channel, timestamp);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), addChirpBroadcast.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ADD_BROADCAST.getAction(), addChirpBroadcast.getAction());
  }

  @Test
  public void getPostIdTest() {
    assertEquals(postId, addChirpBroadcast.getPostId());
  }

  @Test
  public void getChannelTest() {
    assertEquals(channel, addChirpBroadcast.getChannel());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(timestamp, addChirpBroadcast.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(addChirpBroadcast, new AddChirpBroadcast(postId, channel, timestamp));

    String random = "random";
    assertNotEquals(addChirpBroadcast, new AddChirpBroadcast(random, channel, timestamp));
    assertNotEquals(addChirpBroadcast, new AddChirpBroadcast(postId, random, timestamp));
    assertNotEquals(addChirpBroadcast, new AddChirpBroadcast(postId, channel, timestamp + 1));
  }
}
