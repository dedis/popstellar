package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import org.junit.Test;

public class ConsensusElectAcceptTest {

  private final String messageId = "aaa";
  private final ConsensusElectAccept consensusElectAcceptAccept = new ConsensusElectAccept(messageId, true);
  private final ConsensusElectAccept consensusElectAcceptReject = new ConsensusElectAccept(messageId, false);

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
    assertEquals(consensusElectAcceptAccept, new ConsensusElectAccept(messageId, true));
    assertEquals(consensusElectAcceptReject, new ConsensusElectAccept(messageId, false));

    assertNotEquals(consensusElectAcceptAccept, new ConsensusElectAccept("random", true));
    assertNotEquals(consensusElectAcceptAccept, new ConsensusElectAccept(messageId, false));
    assertNotEquals(consensusElectAcceptReject, new ConsensusElectAccept(messageId, true));
  }

}
