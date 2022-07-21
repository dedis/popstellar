package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ConsensusLearnTest {

  private static final MessageID messageId = generateMessageID();
  private static final long timeInSeconds = 1635277619;
  private static final boolean decision = true;
  private static final List<String> acceptorSignatures = Arrays.asList("aaa", "bbb");
  private static final String instanceId = Hash.hash("ccc");
  private static final ConsensusLearn consensusLearn =
      new ConsensusLearn(instanceId, messageId, timeInSeconds, decision, acceptorSignatures);

  @Test
  public void getInstanceIdTest() {
    assertEquals(instanceId, consensusLearn.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, consensusLearn.getMessageId());
  }

  @Test
  public void getAcceptorSignaturesTest() {
    assertEquals(acceptorSignatures, consensusLearn.getAcceptorSignatures());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), consensusLearn.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.LEARN.getAction(), consensusLearn.getAction());
  }

  @Test
  public void getCreationTest() {
    assertEquals(timeInSeconds, consensusLearn.getCreation());
  }

  @Test
  public void getLearnValueTest() {
    LearnValue value = consensusLearn.getLearnValue();

    assertEquals(decision, value.isDecision());

    LearnValue value2 = new LearnValue(decision);
    assertEquals(value, value2);
    assertEquals(value.hashCode(), value2.hashCode());

    assertNotEquals(value, null);
    assertNotEquals(value, new LearnValue(!decision));
  }

  @Test
  public void equalsTest() {
    ConsensusLearn learn2 =
        new ConsensusLearn(
            instanceId, messageId, timeInSeconds, decision, new ArrayList<>(acceptorSignatures));
    assertEquals(consensusLearn, learn2);
    assertEquals(consensusLearn.hashCode(), learn2.hashCode());

    String random = "random";
    assertNotEquals(consensusLearn, null);
    assertNotEquals(
        consensusLearn,
        new ConsensusLearn(random, messageId, timeInSeconds, decision, acceptorSignatures));
    assertNotEquals(
        consensusLearn,
        new ConsensusLearn(
            instanceId,
            Base64DataUtils.generateMessageIDOtherThan(messageId),
            timeInSeconds,
            decision,
            acceptorSignatures));
    assertNotEquals(
        consensusLearn, new ConsensusLearn(instanceId, messageId, 0, decision, acceptorSignatures));
    assertNotEquals(
        consensusLearn,
        new ConsensusLearn(instanceId, messageId, timeInSeconds, !decision, acceptorSignatures));
    assertNotEquals(
        consensusLearn,
        new ConsensusLearn(
            instanceId, messageId, timeInSeconds, decision, Collections.emptyList()));
  }
}
