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
  private static final List<String> acceptors = Arrays.asList("aaa", "bbb");
  private static final ConsensusLearn consensusLearn = new ConsensusLearn(messageId, acceptors);

  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, consensusLearn.getMessageId());
  }

  @Test
  public void getAcceptorsTest() {
    assertEquals(acceptors, consensusLearn.getAcceptors());
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
    assertEquals(consensusLearn, new ConsensusLearn(messageId, new ArrayList<>(acceptors)));

    assertNotEquals(consensusLearn, new ConsensusLearn("random", acceptors));
    assertNotEquals(consensusLearn, new ConsensusLearn(messageId, Collections.emptyList()));
  }
}
