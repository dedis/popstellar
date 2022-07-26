package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConsensusPrepareTest {

  private static final String instanceId = "aaa";
  private static final MessageID messageId = new MessageID("TVNHX0lE");
  private static final long timeInSeconds = 1635277619;
  private static final int proposedTry = 4;

  private static final ConsensusPrepare prepare =
      new ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), prepare.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.PREPARE.getAction(), prepare.getAction());
  }

  @Test
  public void getInstanceIdTest() {
    assertEquals(instanceId, prepare.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, prepare.getMessageId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(timeInSeconds, prepare.getCreation());
  }

  @Test
  public void getPrepareValueTest() {
    PrepareValue value = prepare.getPrepareValue();

    assertEquals(proposedTry, value.getProposedTry());

    PrepareValue value2 = new PrepareValue(proposedTry);
    assertEquals(value, value2);
    assertEquals(value.hashCode(), value2.hashCode());

    assertNotEquals(value, null);
    assertNotEquals(value, new PrepareValue(proposedTry + 1));
  }

  @Test
  public void equalsTest() {
    ConsensusPrepare prepare2 =
        new ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry);
    assertEquals(prepare, prepare2);
    assertEquals(prepare.hashCode(), prepare2.hashCode());

    String random = "random";
    assertNotEquals(prepare, null);
    assertNotEquals(prepare, new ConsensusPrepare(random, messageId, timeInSeconds, proposedTry));
    assertNotEquals(
        prepare,
        new ConsensusPrepare(
            instanceId,
            Base64DataUtils.generateMessageIDOtherThan(messageId),
            timeInSeconds,
            proposedTry));
    assertNotEquals(
        prepare, new ConsensusPrepare(instanceId, messageId, timeInSeconds + 1, proposedTry));
    assertNotEquals(
        prepare, new ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry + 1));
  }

  @Test
  public void toStringTest() {
    assertEquals(
        "ConsensusPrepare{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=PrepareValue{proposed_try=4}}",
        prepare.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(prepare);

    String dir = "protocol/examples/messageData/consensus_prepare/";
    String jsonInvalid1 = JsonTestUtils.loadFile(dir + "wrong_prepare_negative_created_at.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(dir + "wrong_prepare_negative_proposed_try.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
