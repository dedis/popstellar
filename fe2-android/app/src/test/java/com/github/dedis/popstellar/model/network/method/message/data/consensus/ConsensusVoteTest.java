package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import org.junit.Test;

public class ConsensusVoteTest {

  private final String messageId = "aaa";
  private final ConsensusVote consensusVoteAccept = new ConsensusVote(messageId, true);
  private final ConsensusVote consensusVoteReject = new ConsensusVote(messageId, false);

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, consensusVoteAccept.getMessageId());
  }

  @Test
  public void isAcceptTest() {
    assertTrue(consensusVoteAccept.isAccept());
    assertFalse(consensusVoteReject.isAccept());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), consensusVoteAccept.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.PHASE_1_ELECT_ACCEPT.getAction(), consensusVoteAccept.getAction());
  }

  @Test
  public void equalsTest() {
    assertEquals(consensusVoteAccept, new ConsensusVote(messageId, true));
    assertEquals(consensusVoteReject, new ConsensusVote(messageId, false));

    assertNotEquals(consensusVoteAccept, new ConsensusVote("random", true));
    assertNotEquals(consensusVoteAccept, new ConsensusVote(messageId, false));
    assertNotEquals(consensusVoteReject, new ConsensusVote(messageId, true));
  }

}
