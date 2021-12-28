package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.JsonParseException;

import org.junit.Test;

public class ConsensusFailureTest {

  private static final String messageId = "aaa";
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
    assertNotEquals(failure, new ConsensusFailure(instanceId, "random", timeInSeconds));
    assertNotEquals(failure, new ConsensusFailure(instanceId, messageId, 0));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(failure);

    String pathDir = "protocol/examples/messageData/consensus_failure/";
    String jsonInvalid = JsonTestUtils.loadFile(pathDir + "wrong_failure_negative_created_at.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid));
  }
}
