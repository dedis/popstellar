package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonUtilsTest;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageIDOtherThan;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NotifyAddChirpTest {

  private static final MessageID CHIRP_ID = generateMessageID();
  private static final Channel CHANNEL = Channel.fromString("/root/laoId/social/myChannel");
  private static final long TIMESTAMP = 1631280815;

  private static final NotifyAddChirp NOTIFY_ADD_CHIRP =
      new NotifyAddChirp(CHIRP_ID, CHANNEL, TIMESTAMP);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CHIRP.getObject(), NOTIFY_ADD_CHIRP.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.NOTIFY_ADD.getAction(), NOTIFY_ADD_CHIRP.getAction());
  }

  @Test
  public void getChirpIdTest() {
    assertEquals(CHIRP_ID, NOTIFY_ADD_CHIRP.getChirpId());
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
    assertEquals(NOTIFY_ADD_CHIRP, new NotifyAddChirp(CHIRP_ID, CHANNEL, TIMESTAMP));

    String random = "random";
    assertNotEquals(
        NOTIFY_ADD_CHIRP,
        new NotifyAddChirp(generateMessageIDOtherThan(CHIRP_ID), CHANNEL, TIMESTAMP));
    assertNotEquals(
        NOTIFY_ADD_CHIRP, new NotifyAddChirp(CHIRP_ID, Channel.ROOT.subChannel(random), TIMESTAMP));
    assertNotEquals(NOTIFY_ADD_CHIRP, new NotifyAddChirp(CHIRP_ID, CHANNEL, TIMESTAMP + 1));
  }

  @Test
  public void jsonValidationTest() {
    JsonUtilsTest.testData(NOTIFY_ADD_CHIRP);

    String pathDir = "protocol/examples/messageData/chirp_notify_add/";
    String jsonInvalid1 =
        JsonUtilsTest.loadFile(pathDir + "wrong_chirp_notify_add_negative_time.json");
    String jsonInvalid2 =
        JsonUtilsTest.loadFile(pathDir + "wrong_chirp_notify_add_not_base_64_chirp_id.json");
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid2));
  }
}
