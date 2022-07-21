package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.github.dedis.popstellar.model.objects.ElectInstance.State.ACCEPTED;
import static com.github.dedis.popstellar.model.objects.ElectInstance.State.FAILED;
import static com.github.dedis.popstellar.model.objects.ElectInstance.State.STARTING;
import static com.github.dedis.popstellar.model.objects.ElectInstance.State.WAITING;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsensusNodeTest {

  private static final ConsensusKey key = new ConsensusKey("type", "id", "property");
  private static final ConsensusElect elect1 =
      new ConsensusElect(1000, key.getId(), key.getType(), key.getProperty(), "value_1");
  private static final ConsensusElect elect2 =
      new ConsensusElect(2000, key.getId(), key.getType(), key.getProperty(), "value_2");
  private static final ConsensusElect elect3 =
      new ConsensusElect(3000, key.getId(), key.getType(), key.getProperty(), "value_3");
  private static final String instanceId = elect1.getInstanceId();
  private static final Channel channel = Channel.fromString("/root/laoChannel/consensus");
  private static final PublicKey publicKey = generatePublicKey();
  private static final MessageID messageId1 = generateMessageID();
  private static final MessageID messageId2 = generateMessageID();
  private static final MessageID messageId3 = generateMessageID();
  private static final ElectInstance electInstance1 =
      new ElectInstance(messageId1, channel, publicKey, Collections.emptySet(), elect1);
  private static final ElectInstance electInstance2 =
      new ElectInstance(messageId2, channel, publicKey, Collections.emptySet(), elect2);
  private static final ElectInstance electInstance3 =
      new ElectInstance(messageId3, channel, publicKey, Collections.emptySet(), elect3);
  private static final String random = "random";

  @Before
  public void setup() {
    electInstance1.setState(FAILED);
    electInstance3.setState(ACCEPTED);
  }

  @Test
  public void getPublicKeyTest() {
    ConsensusNode node = new ConsensusNode(publicKey);
    assertEquals(publicKey, node.getPublicKey());
  }

  @Test
  public void getLastElectInstanceTest() {
    ConsensusNode node = new ConsensusNode(publicKey);

    assertEquals(Optional.empty(), node.getLastElectInstance(instanceId));

    node.addElectInstance(electInstance1);

    Optional<ElectInstance> opt = node.getLastElectInstance(instanceId);
    assertTrue(opt.isPresent());
    assertEquals(electInstance1, opt.get());

    node.addElectInstance(electInstance2);

    Optional<ElectInstance> opt2 = node.getLastElectInstance(instanceId);
    assertTrue(opt2.isPresent());
    assertEquals(electInstance2, opt2.get());

    assertEquals(Optional.empty(), node.getLastElectInstance(random));
  }

  @Test
  public void getStateTest() {
    ConsensusNode node = new ConsensusNode(publicKey);

    assertEquals(WAITING, node.getState(instanceId));

    node.addElectInstance(electInstance1);
    assertEquals(FAILED, node.getState(instanceId));

    node.addElectInstance(electInstance2);
    assertEquals(STARTING, node.getState(instanceId));

    node.addElectInstance(electInstance3);
    assertEquals(ACCEPTED, node.getState(instanceId));
  }

  @Test
  public void addMessageIdOfAnAcceptedConsensusTest() {
    ConsensusNode node = new ConsensusNode(publicKey);

    assertTrue(node.getAcceptedMessageIds().isEmpty());

    node.addMessageIdOfAnAcceptedElect(messageId1);
    node.addMessageIdOfAnAcceptedElect(messageId2);

    Set<MessageID> messageIds = node.getAcceptedMessageIds();
    assertEquals(2, messageIds.size());
    assertTrue(messageIds.contains(messageId1));
    assertTrue(messageIds.contains(messageId2));
  }
}
