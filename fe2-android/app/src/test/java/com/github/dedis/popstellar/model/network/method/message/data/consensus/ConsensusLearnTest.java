package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConsensusLearnTest {

  private static final String messageId = Hash.hash("aaa");
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
  public void equalsTest() {
    ConsensusLearn learn2 =
        new ConsensusLearn(
            instanceId, messageId, timeInSeconds, decision, new ArrayList<>(acceptorSignatures));
    assertEquals(consensusLearn, learn2);
    assertEquals(consensusLearn.hashCode(), learn2.hashCode());

    String random = "random";
    assertNotEquals(
        consensusLearn,
        new ConsensusLearn(random, messageId, timeInSeconds, decision, acceptorSignatures));
    assertNotEquals(
        consensusLearn,
        new ConsensusLearn(instanceId, random, timeInSeconds, decision, acceptorSignatures));
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
