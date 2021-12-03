package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class AddChirpTest {

  private static final String TEXT = "Hello guys";
  private static final String PARENT_ID = "parentId";
  private static final long TIMESTAMP = 1631280815;

  private static final AddChirp ADD_CHIRP = new AddChirp(TEXT, PARENT_ID, TIMESTAMP);

  @Test
  public void createAddChirpWithTextTooLongTest() {
    String textTooLong =
        "This text should be way over three hundred characters which is the current limit of the"
            + " text within a chirp, and if I try to set the chirp's text to this, it  should"
            + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
            + " screwed something up. But normally it is not that hard to write enough to reach"
            + " the threshold.";
    assertThrows(
        IllegalArgumentException.class, () -> new AddChirp(textTooLong, PARENT_ID, TIMESTAMP));
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), ADD_CHIRP.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ADD.getAction(), ADD_CHIRP.getAction());
  }

  @Test
  public void getTextTest() {
    assertEquals(TEXT, ADD_CHIRP.getText());
  }

  @Test
  public void getParentIdTest() {
    assertEquals(PARENT_ID, ADD_CHIRP.getParentId().get());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, ADD_CHIRP.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(ADD_CHIRP, new AddChirp(TEXT, PARENT_ID, TIMESTAMP));

    String random = "random";
    assertNotEquals(ADD_CHIRP, new AddChirp(random, PARENT_ID, TIMESTAMP));
    assertNotEquals(ADD_CHIRP, new AddChirp(TEXT, random, TIMESTAMP));
    assertNotEquals(ADD_CHIRP, new AddChirp(TEXT, PARENT_ID, TIMESTAMP + 1));
  }
}
