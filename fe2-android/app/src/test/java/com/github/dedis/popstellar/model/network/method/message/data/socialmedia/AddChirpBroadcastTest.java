package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class AddChirpBroadcastTest {

  private static final String POST_ID = "postId";
  private static final String CHANNEL = "/root/laoId/social/myChannel";
  private static final long TIMESTAMP = 1631280815;

  private static final AddChirpBroadcast ADD_CHIRP_BROADCAST =
      new AddChirpBroadcast(POST_ID, CHANNEL, TIMESTAMP);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), ADD_CHIRP_BROADCAST.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ADD_BROADCAST.getAction(), ADD_CHIRP_BROADCAST.getAction());
  }

  @Test
  public void getPostIdTest() {
    assertEquals(POST_ID, ADD_CHIRP_BROADCAST.getPostId());
  }

  @Test
  public void getChannelTest() {
    assertEquals(CHANNEL, ADD_CHIRP_BROADCAST.getChannel());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, ADD_CHIRP_BROADCAST.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(ADD_CHIRP_BROADCAST, new AddChirpBroadcast(POST_ID, CHANNEL, TIMESTAMP));

    String random = "random";
    assertNotEquals(ADD_CHIRP_BROADCAST, new AddChirpBroadcast(random, CHANNEL, TIMESTAMP));
    assertNotEquals(ADD_CHIRP_BROADCAST, new AddChirpBroadcast(POST_ID, random, TIMESTAMP));
    assertNotEquals(ADD_CHIRP_BROADCAST, new AddChirpBroadcast(POST_ID, CHANNEL, TIMESTAMP + 1));
  }
}
