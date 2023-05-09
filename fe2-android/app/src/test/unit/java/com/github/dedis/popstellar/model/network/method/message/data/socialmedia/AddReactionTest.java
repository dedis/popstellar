package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageIDOtherThan;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AddReactionTest {

  private static final String CODE_POINT = "\uD83D\uDC4D";
  private static final MessageID CHIRP_ID = generateMessageID();
  private static final long TIMESTAMP = Instant.now().getEpochSecond();

  private static final AddReaction ADD_REACTION = new AddReaction(CODE_POINT, CHIRP_ID, TIMESTAMP);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.REACTION.getObject(), ADD_REACTION.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ADD.getAction(), ADD_REACTION.getAction());
  }

  @Test
  public void getCodepointTest() {
    assertEquals(CODE_POINT, ADD_REACTION.getCodepoint());
  }

  @Test
  public void getChirpIdTest() {
    assertEquals(CHIRP_ID, ADD_REACTION.getChirpId());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, ADD_REACTION.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(ADD_REACTION, new AddReaction(CODE_POINT, CHIRP_ID, TIMESTAMP));

    assertNotEquals(
        ADD_REACTION, new AddReaction("❤", generateMessageIDOtherThan(CHIRP_ID), TIMESTAMP));
    assertNotEquals(
        ADD_REACTION, new AddReaction(CODE_POINT, generateMessageIDOtherThan(CHIRP_ID), TIMESTAMP));
    assertNotEquals(ADD_REACTION, new AddReaction(CODE_POINT, CHIRP_ID, TIMESTAMP + 1));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(ADD_REACTION);

    String pathDir = "protocol/examples/messageData/reaction_add/";
    String jsonInvalid1 = JsonTestUtils.loadFile(pathDir + "wrong_reaction_add_negative_time.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_reaction_add_not_base_64_chirp_id.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
