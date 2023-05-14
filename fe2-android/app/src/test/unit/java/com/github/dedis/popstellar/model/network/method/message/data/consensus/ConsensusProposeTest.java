package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConsensusProposeTest {

  private static final String instanceId = "aaa";
  private static final MessageID messageId = new MessageID("TVNHX0lE");
  private static final long timeInSeconds = 1635277619;
  private static final int proposedTry = 4;
  private static final boolean proposedValue = true;
  private static final List<String> acceptorSignatures = Arrays.asList("h1", "h2");

  private static final ConsensusPropose propose =
      new ConsensusPropose(
          instanceId, messageId, timeInSeconds, proposedTry, proposedValue, acceptorSignatures);

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), propose.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.PROPOSE.getAction(), propose.getAction());
  }

  @Test
  public void getInstanceIdTest() {
    assertEquals(instanceId, propose.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, propose.getMessageId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(timeInSeconds, propose.getCreation());
  }

  @Test
  public void getProposeValueTest() {
    ProposeValue value = propose.getProposeValue();

    assertEquals(proposedTry, value.getProposedTry());
    assertEquals(proposedValue, value.isProposedValue());

    ProposeValue value2 = new ProposeValue(proposedTry, proposedValue);
    assertEquals(value, value2);
    assertEquals(value.hashCode(), value2.hashCode());

    assertNotEquals(value, null);
    assertNotEquals(value, new ProposeValue(proposedTry + 1, proposedValue));
    assertNotEquals(value, new ProposeValue(proposedTry, !proposedValue));
  }

  @Test
  public void getAcceptorSignaturesTest() {
    assertEquals(acceptorSignatures, propose.getAcceptorSignatures());
  }

  @Test
  public void equalsTest() {
    ConsensusPropose propose2 =
        new ConsensusPropose(
            instanceId,
            messageId,
            timeInSeconds,
            proposedTry,
            proposedValue,
            new ArrayList<>(acceptorSignatures));
    assertEquals(propose, propose2);
    assertEquals(propose.hashCode(), propose2.hashCode());

    String random = "random";
    assertNotEquals(propose, null);
    assertNotEquals(
        propose,
        new ConsensusPropose(
            random, messageId, timeInSeconds, proposedTry, proposedValue, acceptorSignatures));
    assertNotEquals(
        propose,
        new ConsensusPropose(
            instanceId,
            Base64DataUtils.generateMessageIDOtherThan(messageId),
            timeInSeconds,
            proposedTry,
            proposedValue,
            acceptorSignatures));
    assertNotEquals(
        propose,
        new ConsensusPropose(
            instanceId,
            messageId,
            timeInSeconds + 1,
            proposedTry,
            proposedValue,
            acceptorSignatures));
    assertNotEquals(
        propose,
        new ConsensusPropose(
            instanceId,
            messageId,
            timeInSeconds,
            proposedTry + 1,
            proposedValue,
            acceptorSignatures));
    assertNotEquals(
        propose,
        new ConsensusPropose(
            instanceId, messageId, timeInSeconds, proposedTry, !proposedValue, acceptorSignatures));
    assertNotEquals(
        propose,
        new ConsensusPropose(
            instanceId,
            messageId,
            timeInSeconds,
            proposedTry,
            proposedValue,
            Collections.emptyList()));
  }

  @Test
  public void toStringTest() {
    assertEquals(
        "ConsensusPropose{instance_id='aaa', message_id='TVNHX0lE', created_at=1635277619, value=ProposeValue{proposed_try=4, proposed_value=true}, acceptor-signatures=[h1, h2]}",
        propose.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
    JsonTestUtils.testData(propose);

    String dir = "protocol/examples/messageData/consensus_propose/";
    String jsonInvalid1 = JsonTestUtils.loadFile(dir + "wrong_propose_negative_created_at.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(dir + "wrong_propose_negative_proposed_try.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}
