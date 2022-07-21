package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class ConsensusPromiseTest {

  private static final String instanceId = "aaa";
  private static final MessageID messageId = new MessageID("TVNHX0lE");
  private static final long timeInSeconds = 1635277619;
  private static final int acceptedTry = 4;
  private static final boolean acceptedValue = true;
  private static final int promisedTry = 4;

  private static final ConsensusPromise promise =
      new ConsensusPromise(
          instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue, promisedTry);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), promise.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.PROMISE.getAction(), promise.getAction());
  }

  @Test
  public void getInstanceIdTest() {
    assertEquals(instanceId, promise.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, promise.getMessageId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(timeInSeconds, promise.getCreation());
  }

  @Test
  public void getPromiseValueTest() {
    PromiseValue value = promise.getPromiseValue();

    assertEquals(acceptedTry, value.getAcceptedTry());
    assertEquals(acceptedValue, value.isAcceptedValue());
    assertEquals(promisedTry, value.getPromisedTry());

    PromiseValue value2 = new PromiseValue(acceptedTry, acceptedValue, promisedTry);
    assertEquals(value, value2);
    assertEquals(value.hashCode(), value2.hashCode());

    assertNotEquals(value, null);
    assertNotEquals(value, new PromiseValue(acceptedTry + 1, acceptedValue, promisedTry));
    assertNotEquals(value, new PromiseValue(acceptedTry, !acceptedValue, promisedTry));
    assertNotEquals(value, new PromiseValue(acceptedTry, acceptedValue, promisedTry + 1));
  }

  @Test
  public void equalsTest() {
    ConsensusPromise promise2 =
        new ConsensusPromise(
            instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue, promisedTry);
    assertEquals(promise, promise2);
    assertEquals(promise.hashCode(), promise2.hashCode());

    String random = "random";
    assertNotEquals(promise, null);
    assertNotEquals(
        promise,
        new ConsensusPromise(
            random, messageId, timeInSeconds, acceptedTry, acceptedValue, promisedTry));
    assertNotEquals(
        promise,
        new ConsensusPromise(
            instanceId,
            Base64DataUtils.generateMessageIDOtherThan(messageId),
            timeInSeconds,
            acceptedTry,
            acceptedValue,
            promisedTry));
    assertNotEquals(
        promise,
        new ConsensusPromise(
            instanceId, messageId, timeInSeconds + 1, acceptedTry, acceptedValue, promisedTry));
    assertNotEquals(
        promise,
        new ConsensusPromise(
            instanceId, messageId, timeInSeconds, acceptedTry + 1, acceptedValue, promisedTry));
    assertNotEquals(
        promise,
        new ConsensusPromise(
            instanceId, messageId, timeInSeconds, acceptedTry, !acceptedValue, promisedTry));
    assertNotEquals(
        promise,
        new ConsensusPromise(
            instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue, promisedTry + 1));
  }

  @Test
  public void toStringTest() {
    assertEquals(
        "ConsensusPromise{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=PromiseValue{accepted_try=4, accepted_value=true, promised_try=4}}",
        promise.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(promise);

    String dir = "protocol/examples/messageData/consensus_promise/";
    String jsonInvalid1 = JsonTestUtils.loadFile(dir + "wrong_promise_negative_created_at.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(dir + "wrong_promise_negative_accepted_try.json");
    String jsonInvalid3 = JsonTestUtils.loadFile(dir + "wrong_promise_negative_promised_try.json");

    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid3));
  }
}
