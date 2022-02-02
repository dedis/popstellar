package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.model.objects.ConsensusNode.State.ACCEPTED;
import static com.github.dedis.popstellar.model.objects.ConsensusNode.State.FAILED;
import static com.github.dedis.popstellar.model.objects.ConsensusNode.State.STARTING;
import static com.github.dedis.popstellar.model.objects.ConsensusNode.State.WAITING;
import static com.github.dedis.popstellar.testutils.fragment.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.fragment.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

public class ConsensusNodeTest {

  private static final ConsensusKey key = new ConsensusKey("type", "id", "property");
  private static final Consensus consensus1 = new Consensus(1000, key, "value_1");
  private static final Consensus consensus2 = new Consensus(2000, key, "value_2");
  private static final Consensus consensus3 = new Consensus(3000, key, "value_3");
  private static final String instanceId = consensus1.getId();
  private static final PublicKey publicKey = generatePublicKey();
  private static final MessageID messageId1 = generateMessageID();
  private static final MessageID messageId2 = generateMessageID();
  private static final MessageID messageId3 = generateMessageID();
  private static final String random = "random";

  @Before
  public void setup() {
    consensus1.setMessageId(messageId1);
    consensus2.setMessageId(messageId2);
    consensus3.setMessageId(messageId3);

    consensus1.setFailed(true);
    consensus3.setAccepted(true);
  }

  @Test
  public void getPublicKeyTest() {
    ConsensusNode node = new ConsensusNode(publicKey);
    assertEquals(publicKey, node.getPublicKey());
  }

  @Test
  public void getLastConsensusTest() {
    ConsensusNode node = new ConsensusNode(publicKey);

    assertEquals(Optional.empty(), node.getLastConsensus(instanceId));

    node.addConsensus(consensus1);

    Optional<Consensus> opt = node.getLastConsensus(instanceId);
    assertTrue(opt.isPresent());
    assertEquals(consensus1, opt.get());

    node.addConsensus(consensus2);

    Optional<Consensus> opt2 = node.getLastConsensus(instanceId);
    assertTrue(opt2.isPresent());
    assertEquals(consensus2, opt2.get());

    assertEquals(Optional.empty(), node.getLastConsensus(random));
  }

  @Test
  public void getStateTest() {
    ConsensusNode node = new ConsensusNode(publicKey);

    assertEquals(WAITING, node.getState(instanceId));

    node.addConsensus(consensus1);
    assertEquals(FAILED, node.getState(instanceId));

    node.addConsensus(consensus2);
    assertEquals(STARTING, node.getState(instanceId));

    node.addConsensus(consensus3);
    assertEquals(ACCEPTED, node.getState(instanceId));
  }

  @Test
  public void addMessageIdOfAnAcceptedConsensusTest() {
    ConsensusNode node = new ConsensusNode(publicKey);

    assertTrue(node.getAcceptedMessageIds().isEmpty());

    node.addMessageIdOfAnAcceptedConsensus(messageId1);
    node.addMessageIdOfAnAcceptedConsensus(messageId2);

    Set<MessageID> messageIds = node.getAcceptedMessageIds();
    assertEquals(2, messageIds.size());
    assertTrue(messageIds.contains(messageId1));
    assertTrue(messageIds.contains(messageId2));
  }
}
