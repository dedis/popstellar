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
public class DeleteChirpTest {

  private static final MessageID CHIRP_ID = generateMessageID();
  private static final long TIMESTAMP = 1631280815;

  private static final DeleteChirp DELETE_CHIRP = new DeleteChirp(CHIRP_ID, TIMESTAMP);

  @Before
  public void setup() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), DELETE_CHIRP.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.DELETE.getAction(), DELETE_CHIRP.getAction());
  }

  @Test
  public void getChirpIdTest() {
    assertEquals(CHIRP_ID, DELETE_CHIRP.getChirpId());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, DELETE_CHIRP.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(DELETE_CHIRP, new DeleteChirp(CHIRP_ID, TIMESTAMP));

    assertNotEquals(DELETE_CHIRP, new DeleteChirp(generateMessageIDOtherThan(CHIRP_ID), TIMESTAMP));
    assertNotEquals(DELETE_CHIRP, new DeleteChirp(CHIRP_ID, TIMESTAMP + 1));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(DELETE_CHIRP);

    String pathDir = "protocol/examples/messageData/chirp_delete_publish/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_chirp_delete_publish_negative_time.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_chirp_delete_publish_not_base_64_chirp_id.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
