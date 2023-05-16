package com.github.dedis.popstellar.model.network.method.message.data.socialmedia;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonUtilsTest;
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
public class DeleteReactionTest {

  private static final MessageID REACTION_ID = generateMessageID();
  private static final long TIMESTAMP = Instant.now().getEpochSecond();

  private static final DeleteReaction DELETE_REACTION = new DeleteReaction(REACTION_ID, TIMESTAMP);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.REACTION.getObject(), DELETE_REACTION.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.DELETE.getAction(), DELETE_REACTION.getAction());
  }

  @Test
  public void getChirpIdTest() {
    assertEquals(REACTION_ID, DELETE_REACTION.getReactionID());
  }

  @Test
  public void getTimestampTest() {
    assertEquals(TIMESTAMP, DELETE_REACTION.getTimestamp());
  }

  @Test
  public void equalsTest() {
    assertEquals(DELETE_REACTION, new DeleteReaction(REACTION_ID, TIMESTAMP));

    assertNotEquals(
        DELETE_REACTION, new DeleteReaction(generateMessageIDOtherThan(REACTION_ID), TIMESTAMP));
    assertNotEquals(DELETE_REACTION, new DeleteReaction(REACTION_ID, TIMESTAMP + 1));
  }

  @Test
  public void jsonValidationTest() {
    JsonUtilsTest.testData(DELETE_REACTION);

    String pathDir = "protocol/examples/messageData/reaction_delete/";
    String jsonInvalid1 =
        JsonUtilsTest.loadFile(pathDir + "wrong_reaction_delete_negative_time.json");
    String jsonInvalid2 =
        JsonUtilsTest.loadFile(pathDir + "wrong_reaction_delete_not_base_64_reaction_id.json");
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid2));
  }
}
