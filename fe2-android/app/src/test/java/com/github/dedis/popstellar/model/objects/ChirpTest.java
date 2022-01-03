package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

public class ChirpTest {
  private static final MessageID ID = generateMessageID();

  private static final Chirp CHIRP = new Chirp(ID);

  @Test
  public void createChirpWithNullId() {
    assertThrows(IllegalArgumentException.class, () -> new Chirp(null));
  }

  @Test
  public void createChirpWithEmptyId() {
    assertThrows(IllegalArgumentException.class, () -> new Chirp(new MessageID("")));
  }

  @Test
  public void setAndGetIdTest() {
    MessageID newId = generateMessageID();
    CHIRP.setId(newId);
    assertEquals(newId, CHIRP.getId());

    assertThrows(IllegalArgumentException.class, () -> CHIRP.setId(null));
    assertThrows(IllegalArgumentException.class, () -> CHIRP.setId(new MessageID("")));
  }

  @Test
  public void setAndGetChannelTest() {
    String channel = "/root/laoId/social/myChannel";
    CHIRP.setChannel(channel);
    assertEquals(channel, CHIRP.getChannel());
  }

  @Test
  public void setAndGetSenderTest() {
    PublicKey sender = generatePublicKey();
    CHIRP.setSender(sender);
    assertEquals(sender, CHIRP.getSender());
  }

  @Test
  public void setAndGetTextTest() {
    String text = "Hello everyone, hope you enjoy your day";
    CHIRP.setText(text);
    assertEquals(text, CHIRP.getText());

    String textTooLong =
        "This text should be way over three hundred characters which is the current limit of the"
            + " text within a chirp, and if I try to set the chirp's text to this, it  should"
            + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
            + " screwed something up. But normally it is not that hard to write enough to reach"
            + " the threshold.";
    assertThrows(IllegalArgumentException.class, () -> CHIRP.setText(textTooLong));
  }

  @Test
  public void setAndGetTimestampTest() {
    long timestamp = 1631280815;
    CHIRP.setTimestamp(timestamp);
    assertEquals(timestamp, CHIRP.getTimestamp());
  }

  @Test
  public void setAndGetLikesTest() {
    int likes = 2021;
    CHIRP.setLikes(likes);
    assertEquals(likes, CHIRP.getLikes());
  }

  @Test
  public void setAndGetParentId() {
    MessageID parentId = generateMessageID();
    CHIRP.setParentId(parentId);
    assertEquals(parentId, CHIRP.getParentId());
  }
}
