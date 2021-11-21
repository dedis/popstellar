package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class AddChirpTest {

  private static final String text = "Hello guys";
  private static final String parentId = "parentId";
  private static final long timestamp = 1631280815;

  private static final AddChirp addChirp = new AddChirp(text, parentId, timestamp);

  @Test
  public void createAddChirpWithTextTooLongTest() {
    String textTooLong =
        "This text should be way over three hundred characters which is the current limit of the"
            + " text within a chirp, and if I try to set the chirp's text to this, it  should"
            + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
            + " screwed something up. But normally it is not that hard to write enough to reach"
            + " the threshold.";
    assertThrows(
        IllegalArgumentException.class, () -> new AddChirp(textTooLong, parentId, timestamp));
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), addChirp.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ADD.getAction(), addChirp.getAction());
  }

  @Test
  public void getTextTest() {
    assertEquals(text, addChirp.getText());
  }

  @Test
  public void getParentIdTest() {
    assertEquals(parentId, addChirp.getParentId().get());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(timestamp, addChirp.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(addChirp, new AddChirp(text, parentId, timestamp));

    String random = "random";
    assertNotEquals(addChirp, new AddChirp(random, parentId, timestamp));
    assertNotEquals(addChirp, new AddChirp(text, random, timestamp));
    assertNotEquals(addChirp, new AddChirp(text, parentId, timestamp + 1));
  }
}
