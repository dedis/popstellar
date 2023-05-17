package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConsensusAcceptTest {

  private static final String instanceId = "aaa";
  private static final MessageID messageId = new MessageID("TVNHX0lE");
  private static final long timeInSeconds = 1635277619;
  private static final int acceptedTry = 4;
  private static final boolean acceptedValue = true;

  private static final ConsensusAccept accept =
      new ConsensusAccept(instanceId, messageId, timeInSeconds, acceptedTry, acceptedValue);

  @Before
  public void setup() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
  }

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
        accept,
        new ConsensusAccept(
            instanceId,
            Base64DataUtils.generateMessageIDOtherThan(messageId),
            timeInSeconds,
            acceptedTry,
            acceptedValue));
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
        "ConsensusAccept{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=AcceptValue{accepted_try=4, accepted_value=true}}",
        accept.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(accept);

    String pathDir = "protocol/examples/messageData/consensus_accept/";
    String jsonInvalid1 = JsonTestUtils.loadFile(pathDir + "wrong_accept_negative_created_at.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_accept_negative_accepted_try.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
