package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class ConsensusAcceptTest {

  private static final String instanceId = "aaa";
  private static final String messageId = "bbb";
  private static final long timeInSeconds = 1635277619;
  private static final int acceptedTry = 4;
  private static final boolean acceptedValue = true;

  private static final ConsensusAccept accept =
      new ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), accept.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ACCEPT.getAction(), accept.getAction());
  }

  @Test
  public void getInstanceIdTest() {
    assertEquals(instanceId, accept.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, accept.getMessageId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(timeInSeconds, accept.getCreation());
  }

  @Test
  public void getAcceptValueTest() {
    AcceptValue value = accept.getAcceptValue();

    assertEquals(acceptedTry, value.getAcceptedTry());
    assertEquals(acceptedValue, value.isAcceptedValue());

    AcceptValue value2 = new AcceptValue(acceptedTry, acceptedValue);
    assertEquals(value, value2);
    assertEquals(value.hashCode(), value2.hashCode());

    assertNotEquals(value, null);
    assertNotEquals(value, new AcceptValue(acceptedTry + 1, acceptedValue));
    assertNotEquals(value, new AcceptValue(acceptedTry, !acceptedValue));
  }

  @Test
  public void equalsTest() {
    ConsensusAccept accept2 =
        new ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue);
    assertEquals(accept, accept2);
    assertEquals(accept.hashCode(), accept2.hashCode());

    String random = "random";
    assertNotEquals(accept, null);
    assertNotEquals(
        accept, new ConsensusAccept(random, messageId, timeInSeconds, acceptedTry, acceptedValue));
    assertNotEquals(
        accept, new ConsensusAccept(instanceId, random, timeInSeconds, acceptedTry, acceptedValue));
    assertNotEquals(
        accept,
        new ConsensusAccept(instanceId, messageId, timeInSeconds + 1, acceptedTry, acceptedValue));
    assertNotEquals(
        accept,
        new ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry + 1, acceptedValue));
    assertNotEquals(
        accept,
        new ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, !acceptedValue));
  }

  @Test
  public void toStringTest() {
    assertEquals(
        "ConsensusAccept{instance_id='aaa', message_id='bbb', created_at=1635277619, value=AcceptValue{accepted_try=4, accepted_value=true}}",
        accept.toString());
  }
}
