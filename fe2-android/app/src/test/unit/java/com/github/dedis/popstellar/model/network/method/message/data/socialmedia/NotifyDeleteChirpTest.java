package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageIDOtherThan;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NotifyDeleteChirpTest {

  private static final MessageID CHIRP_ID = generateMessageID();
  private static final String CHANNEL = "/root/laoId/social/myChannel";
  private static final long TIMESTAMP = 1631280815;

  private static final NotifyDeleteChirp NOTIFY_DELETE_CHIRP =
      new NotifyDeleteChirp(CHIRP_ID, CHANNEL, TIMESTAMP);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), NOTIFY_DELETE_CHIRP.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.NOTIFY_DELETE.getAction(), NOTIFY_DELETE_CHIRP.getAction());
  }

  @Test
  public void getChirpIdTest() {
    assertEquals(CHIRP_ID, NOTIFY_DELETE_CHIRP.getChirpId());
  }

  @Test
  public void getChannelTest() {
    assertEquals(CHANNEL, NOTIFY_DELETE_CHIRP.getChannel());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, NOTIFY_DELETE_CHIRP.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(NOTIFY_DELETE_CHIRP, new NotifyDeleteChirp(CHIRP_ID, CHANNEL, TIMESTAMP));

    String random = "random";
    assertNotEquals(
        NOTIFY_DELETE_CHIRP,
        new NotifyDeleteChirp(generateMessageIDOtherThan(CHIRP_ID), CHANNEL, TIMESTAMP));
    assertNotEquals(NOTIFY_DELETE_CHIRP, new NotifyDeleteChirp(CHIRP_ID, random, TIMESTAMP));
    assertNotEquals(NOTIFY_DELETE_CHIRP, new NotifyDeleteChirp(CHIRP_ID, CHANNEL, TIMESTAMP + 1));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(NOTIFY_DELETE_CHIRP);

    String pathDir = "protocol/examples/messageData/chirp_notify_delete/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_chirp_notify_delete_negative_time.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_chirp_notify_delete_not_base_64_chirp_id.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
