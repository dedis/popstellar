package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.JsonParseException;

import org.junit.Test;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageIDOtherThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

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
    JsonTestUtils.testData(NOTIFY_ADD_CHIRP);

    String pathDir = "protocol/examples/messageData/chirp_notify_add/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_chirp_notify_add_negative_time.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_chirp_notify_add_not_base_64_chirp_id.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
