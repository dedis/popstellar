package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.fragment.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.fragment.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConsensusTest {

  private static final long creationInSeconds = 1635277619;
  private static final String type = "TestType";
  private static final String objId = Hash.hash("TestId");
  private static final String property = "TestProperty";

  private static final ConsensusKey key = new ConsensusKey(type, objId, property);
  private static final Object value = "TestValue";

  private static final Consensus consensus = new Consensus(creationInSeconds, key, value);

  @Test
  public void setAndGetMessageIdTest() {
    MessageID messageId = generateMessageID();
    consensus.setMessageId(messageId);
    assertEquals(messageId, consensus.getMessageId());
  }

  @Test
  public void setAndGetChannelTest() {
    String channel = "/root/aaa/consensus";
    consensus.setChannel(channel);
    assertEquals(channel, consensus.getChannel());
  }

  @Test
  public void setAndGetIdTest() {
    // Hash("consensus"||key:type||key:id||key:property)
    String expectedId = Hash.hash("consensus", type, objId, property);
    assertEquals(expectedId, consensus.getId());

    String newId = Hash.hash("newId");
    consensus.setId(newId);
    assertEquals(newId, consensus.getId());
  }

  @Test
  public void setAndGetKeyTest() {
    assertEquals(key, consensus.getKey());
    ConsensusKey newKey = new ConsensusKey("type2", "id2", "property2");
    consensus.setKey(newKey);
    assertEquals(newKey, consensus.getKey());
  }

  @Test
  public void setAndGetCreationTest() {
    assertEquals(creationInSeconds, consensus.getCreation());

    long newCreation = creationInSeconds + 10;
    consensus.setCreation(newCreation);
    assertEquals(newCreation, consensus.getCreation());
  }

  @Test
  public void setAndGetProposerTest() {
    PublicKey proposer = generatePublicKey();
    consensus.setProposer(proposer);
    assertEquals(proposer, consensus.getProposer());
  }

  @Test
  public void setAndGetNodesTest() {
    Set<PublicKey> nodes = new HashSet<>();
    consensus.setNodes(nodes);
    assertEquals(nodes, consensus.getNodes());
  }

  @Test
  public void acceptorsResponsesTest() {
    PublicKey acceptor1 = generatePublicKey();
    MessageID messageId1 = generateMessageID();
    consensus.putPositiveAcceptorResponse(acceptor1, messageId1);

    Map<PublicKey, MessageID> messageIds = consensus.getAcceptorsToMessageId();
    assertEquals(1, messageIds.size());
    assertEquals(messageId1, messageIds.get(acceptor1));
  }

  @Test
  public void setAndGetAcceptedTest() {
    assertFalse(consensus.isAccepted());

    consensus.setAccepted(true);
    assertTrue(consensus.isAccepted());

    consensus.setAccepted(false);
    assertFalse(consensus.isAccepted());
  }

  @Test
  public void setAndGetFailedTest() {
    assertFalse(consensus.isFailed());

    consensus.setFailed(true);
    assertTrue(consensus.isFailed());

    consensus.setFailed(false);
    assertFalse(consensus.isFailed());
  }

  @Test
  public void generateConsensusIdTest() {
    // Hash(“consensus”||key:type||key:id||key:property)
    String expectedId = Hash.hash("consensus", type, objId, property);
    assertEquals(expectedId, Consensus.generateConsensusId(type, objId, property));
  }
}
