package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class NotifyAddChirpTest {

  private static final String POST_ID = "postId";
  private static final String CHANNEL = "/root/laoId/social/myChannel";
  private static final long TIMESTAMP = 1631280815;

  private static final NotifyAddChirp NOTIFY_ADD_CHIRP =
      new NotifyAddChirp(POST_ID, CHANNEL, TIMESTAMP);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), NOTIFY_ADD_CHIRP.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.NOTIFY_ADD.getAction(), NOTIFY_ADD_CHIRP.getAction());
  }

  @Test
  public void getPostIdTest() {
    assertEquals(POST_ID, NOTIFY_ADD_CHIRP.getPostId());
  }

  @Test
  public void getChannelTest() {
    assertEquals(CHANNEL, NOTIFY_ADD_CHIRP.getChannel());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, NOTIFY_ADD_CHIRP.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(NOTIFY_ADD_CHIRP, new NotifyAddChirp(POST_ID, CHANNEL, TIMESTAMP));

    String random = "random";
    assertNotEquals(NOTIFY_ADD_CHIRP, new NotifyAddChirp(random, CHANNEL, TIMESTAMP));
    assertNotEquals(NOTIFY_ADD_CHIRP, new NotifyAddChirp(POST_ID, random, TIMESTAMP));
    assertNotEquals(NOTIFY_ADD_CHIRP, new NotifyAddChirp(POST_ID, CHANNEL, TIMESTAMP + 1));
  }
}
