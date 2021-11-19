package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ChirpTest {
  private static final String id = "messageId";

  private static final Chirp chirp = new Chirp(id);

  @Test
  public void createChirpWithNullId() {
    assertThrows(IllegalArgumentException.class, () -> new Chirp(null));
  }

  @Test
  public void createChirpWithEmptyId() {
    assertThrows(IllegalArgumentException.class, () -> new Chirp(""));
  }

  @Test
  public void setAndGetIdTest() {
    String newId = "newMessageId";
    chirp.setId(newId);
    assertEquals(newId, chirp.getId());
  }

  @Test
  public void setAndGetChannelTest() {
    String channel = "/root/laoId/social/myChannel";
    chirp.setChannel(channel);
    assertEquals(channel, chirp.getChannel());
  }

  @Test
  public void setAndGetSenderTest() {
    String sender = "senderPublicKey";
    chirp.setSender(sender);
    assertEquals(sender, chirp.getSender());
  }

  @Test
  public void setAndGetTextTest() {
    String text = "Hello everyone, hope you enjoy your day";
    chirp.setText(text);
    assertEquals(text, chirp.getText());

    String textTooLong =
        "This text should be way over three hundred characters which is the current limit of the"
            + " text within a chirp, and if I try to set the chirp's text to this, it  should"
            + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
            + " screwed something up. But normally it is not that hard to write enough to reach"
            + " the threshold.";
    assertThrows(IllegalArgumentException.class, () -> chirp.setText(textTooLong));
  }

  @Test
  public void setAndGetTimestampTest() {
    long timestamp = 1631280815;
    chirp.setTimestamp(timestamp);
    assertEquals(timestamp, chirp.getTimestamp());
  }

  @Test
  public void setAndGetLikesTest() {
    int likes = 2021;
    chirp.setLikes(likes);
    assertEquals(likes, chirp.getLikes());
  }

  @Test
  public void setAndGetParentId() {
    String parentId = "theParentId";
    chirp.setParentId(parentId);
    assertEquals(parentId, chirp.getParentId());
  }
}
