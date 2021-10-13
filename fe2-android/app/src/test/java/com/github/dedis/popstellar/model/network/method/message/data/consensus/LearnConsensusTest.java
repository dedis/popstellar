package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.utility.security.Hash;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class LearnConsensusTest {

  private final String messageId = Hash.hash("aaa");
  private final List<String> acceptors = Arrays.asList("aaa", "bbb");
  private final LearnConsensus learnConsensus = new LearnConsensus(messageId, acceptors);


  @Test
  public void getMessageIdTest() {
    assertEquals(messageId, learnConsensus.getMessageId());
  }

  @Test
  public void getAcceptorsTest() {
    assertEquals(acceptors, learnConsensus.getAcceptors());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), learnConsensus.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.PHASE_1_LEARN.getAction(), learnConsensus.getAction());
  }

  @Test
  public void equalsTest() {
    assertEquals(learnConsensus, new LearnConsensus(messageId, new ArrayList<>(acceptors)));

    assertNotEquals(learnConsensus, new LearnConsensus("random", acceptors));
    assertNotEquals(learnConsensus, new LearnConsensus(messageId, Collections.emptyList()));
  }


}
