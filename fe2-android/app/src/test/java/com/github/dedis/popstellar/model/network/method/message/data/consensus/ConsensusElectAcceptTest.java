package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

public class ConsensusElectAcceptTest {

  private static final String messageId = "aaa";
  private static final String instanceId = "bbb";
  private static final ConsensusElectAccept consensusElectAcceptAccept =
      new ConsensusElectAccept(instanceId, messageId, true);
  private static final ConsensusElectAccept consensusElectAcceptReject =
      new ConsensusElectAccept(instanceId, messageId, false);

  @Test
  public void getInstanceId() {
    assertEquals(instanceId, consensusElectAcceptAccept.getInstanceId());
  }

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, consensusElectAcceptAccept.getMessageId());
  }

  @Test
  public void isAcceptTest() {
    assertTrue(consensusElectAcceptAccept.isAccept());
    assertFalse(consensusElectAcceptReject.isAccept());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), consensusElectAcceptAccept.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ELECT_ACCEPT.getAction(), consensusElectAcceptAccept.getAction());
  }

  @Test
  public void equalsTest() {
    assertEquals(consensusElectAcceptAccept, new ConsensusElectAccept(instanceId, messageId, true));
    assertEquals(consensusElectAcceptReject, new ConsensusElectAccept(instanceId, messageId, false));

    assertNotEquals(consensusElectAcceptAccept, new ConsensusElectAccept("random", messageId, true));
    assertNotEquals(consensusElectAcceptAccept, new ConsensusElectAccept(instanceId, "random", true));
    assertNotEquals(consensusElectAcceptAccept, new ConsensusElectAccept(instanceId, messageId, false));
    assertNotEquals(consensusElectAcceptReject, new ConsensusElectAccept(instanceId, messageId, true));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(consensusElectAcceptAccept);
  }
}
