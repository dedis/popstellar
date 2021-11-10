package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class ConsensusPrepareTest {

  private static final String instanceId = "aaa";
  private static final String messageId = "bbb";
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
    assertEquals(value, value);
    assertEquals(value, value2);
    assertEquals(value.hashCode(), value2.hashCode());

    assertNotEquals(value, null);
    assertNotEquals(value, new PrepareValue(proposedTry + 1));
  }

  @Test
  public void equalsTest() {
    ConsensusPrepare prepare2 =
        new ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry);
    assertEquals(prepare, prepare);
    assertEquals(prepare, prepare2);
    assertEquals(prepare.hashCode(), prepare2.hashCode());

    String random = "random";
    assertNotEquals(prepare, null);
    assertNotEquals(prepare, new ConsensusPrepare(random, messageId, timeInSeconds, proposedTry));
    assertNotEquals(prepare, new ConsensusPrepare(instanceId, random, timeInSeconds, proposedTry));
    assertNotEquals(
        prepare, new ConsensusPrepare(instanceId, messageId, timeInSeconds + 1, proposedTry));
    assertNotEquals(
        prepare, new ConsensusPrepare(instanceId, messageId, timeInSeconds, proposedTry + 1));
  }

  @Test
  public void toStringTest() {
    assertEquals(
        "ConsensusPrepare{instance_id='aaa', message_id='bbb', created_at=1635277619, value=PrepareValue{proposed_try=4}}",
        prepare.toString());
  }
}
