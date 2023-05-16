package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonUtilsTest;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConsensusFailureTest {

  private static final MessageID messageId = generateMessageID();
  private static final String instanceId = "bbb";
  private static final long timeInSeconds = 1635277619;
  private static final ConsensusFailure failure =
      new ConsensusFailure(instanceId, messageId, timeInSeconds);

  @Test
  public void getInstanceId() {
    assertEquals(instanceId, failure.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, failure.getMessageId());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), failure.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.FAILURE.getAction(), failure.getAction());
  }

  @Test
  public void equalsTest() {
    ConsensusFailure failure2 = new ConsensusFailure(instanceId, messageId, timeInSeconds);
    assertEquals(failure, failure2);
    assertEquals(failure.hashCode(), failure2.hashCode());

    assertNotEquals(failure, new ConsensusFailure("random", messageId, timeInSeconds));
    assertNotEquals(
        failure,
        new ConsensusFailure(
            instanceId, Base64DataUtils.generateMessageIDOtherThan(messageId), timeInSeconds));
    assertNotEquals(failure, new ConsensusFailure(instanceId, messageId, 0));
  }

  @Test
  public void jsonValidationTest() {
    JsonUtilsTest.testData(failure);

    String pathDir = "protocol/examples/messageData/consensus_failure/";
    String jsonInvalid = JsonUtilsTest.loadFile(pathDir + "wrong_failure_negative_created_at.json");
    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid));
  }
}
