package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Map;
import java.util.Set;

import static com.github.dedis.popstellar.model.objects.ElectInstance.State.STARTING;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.assertEquals;

public class ElectInstanceTest {

  private static final long creationInSeconds = 1635277619;
  private static final String type = "TestType";
  private static final String objId = Hash.hash("TestId");
  private static final String property = "TestProperty";

  private static final ConsensusKey key = new ConsensusKey(type, objId, property);
  private static final Object value = "TestValue";

  private static final MessageID electMessageId = generateMessageID();
  private static final Channel channel = Channel.fromString("/root/aaa/consensus");
  private static final PublicKey proposer = generatePublicKey();
  private static final PublicKey node2 = generatePublicKey();
  private static final PublicKey node3 = generatePublicKey();
  private static final Set<PublicKey> nodes = Sets.newSet(proposer, node2, node3);
  private static final ConsensusElect elect =
      new ConsensusElect(creationInSeconds, objId, type, property, value);
  private static final ElectInstance electInstance =
      new ElectInstance(electMessageId, channel, proposer, nodes, elect);

  @Test
  public void getMessageIdTest() {
    assertEquals(electMessageId, electInstance.getMessageId());
  }

  @Test
  public void getChannelTest() {
    assertEquals(channel, electInstance.getChannel());
  }

  @Test
  public void getInstanceIdTest() {
    // Hash("consensus"||key:type||key:id||key:property)
    String expectedId = Hash.hash("consensus", type, objId, property);
    assertEquals(expectedId, electInstance.getInstanceId());
  }

  @Test
  public void getKeyTest() {
    assertEquals(key, electInstance.getKey());
  }

  @Test
  public void getValueTest() {
    assertEquals(value, electInstance.getValue());
  }

  @Test
  public void getCreationTest() {
    assertEquals(creationInSeconds, electInstance.getCreation());
  }

  @Test
  public void getProposerTest() {
    assertEquals(proposer, electInstance.getProposer());
  }

  @Test
  public void getNodesTest() {
    assertEquals(nodes, electInstance.getNodes());
  }

  @Test
  public void acceptorsResponsesTest() {
    MessageID messageId1 = generateMessageID();
    MessageID messageId2 = generateMessageID();
    String instanceId = electInstance.getInstanceId();

    ConsensusElectAccept electAccept = new ConsensusElectAccept(instanceId, electMessageId, true);
    ConsensusElectAccept electReject = new ConsensusElectAccept(instanceId, electMessageId, false);

    electInstance.addElectAccept(node2, messageId1, electAccept);
    electInstance.addElectAccept(node2, messageId2, electReject);

    Map<PublicKey, MessageID> messageIds = electInstance.getAcceptorsToMessageId();
    assertEquals(1, messageIds.size());
    assertEquals(messageId1, messageIds.get(node2));
  }

  @Test
  public void setAndGetStateTest() {
    // default state should be STARTING
    assertEquals(STARTING, electInstance.getState());

    for (ElectInstance.State state : ElectInstance.State.values()) {
      electInstance.setState(state);
      assertEquals(state, electInstance.getState());
    }
  }

  @Test
  public void generateConsensusIdTest() {
    // Hash(“consensus”||key:type||key:id||key:property)
    String expectedId = Hash.hash("consensus", type, objId, property);
    assertEquals(expectedId, ElectInstance.generateConsensusId(type, objId, property));
  }
}
