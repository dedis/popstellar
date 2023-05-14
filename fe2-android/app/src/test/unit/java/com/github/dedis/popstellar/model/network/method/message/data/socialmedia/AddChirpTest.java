package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.JsonParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageIDOtherThan;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AddChirpTest {

  private static final String TEXT = "Hello guys";
  private static final MessageID PARENT_ID = generateMessageID();
  private static final long TIMESTAMP = 1631280815;

  private static final AddChirp ADD_CHIRP = new AddChirp(TEXT, PARENT_ID, TIMESTAMP);

  @Before
  public void setup() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
  }

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
    assertNotEquals(
        ADD_CHIRP, new AddChirp(TEXT, generateMessageIDOtherThan(PARENT_ID), TIMESTAMP));
    assertNotEquals(ADD_CHIRP, new AddChirp(TEXT, PARENT_ID, TIMESTAMP + 1));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(ADD_CHIRP);

    String path =
        "protocol/examples/messageData/chirp_add_publish/wrong_chirp_add_publish_negative_time.json";
    String invalidJson = JsonTestUtils.loadFile(path);
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(invalidJson));
  }
}
