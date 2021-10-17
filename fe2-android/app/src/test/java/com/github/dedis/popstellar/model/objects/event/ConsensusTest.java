package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class ConsensusTest {

  private final long creation = Instant.now().getEpochSecond();
  private final String type = "TestType";
  private final String objId = Hash.hash("TestId");
  private final String property = "TestProperty";

  private final ConsensusKey key = new ConsensusKey(type, objId, property);
  private final Object value = "TestValue";

  private final Consensus consensus = new Consensus(creation, key, value);

  @Test
  public void setAndGetMessageIdTest() {
    String messageId = "aaa";
    consensus.setMessageId(messageId);
    assertEquals(messageId, consensus.getMessageId());
  }

  @Test
  public void setAndGetChannelTest() {
    String channel = "/root/aaa/consensus";
    consensus.setChannel(channel);
    assertEquals(channel, consensus.getChannel());

    assertThrows(IllegalArgumentException.class, () -> consensus.setChannel(null));
  }

  @Test
  public void setAndGetIdTest() {
    // Hash("consensus"||created_at||key:type||key:id||key:property||value)
    String expectedId =
        Hash.hash(
            "consensus", Long.toString(creation), type, objId, property, String.valueOf(value));
    assertEquals(expectedId, consensus.getId());

    String newId = Hash.hash("newId");
    consensus.setId(newId);
    assertEquals(newId, consensus.getId());

    assertThrows(IllegalArgumentException.class, () -> consensus.setId(null));
  }

  @Test
  public void setAndGetKeyTest() {
    assertEquals(key, consensus.getKey());
    ConsensusKey newKey = new ConsensusKey("type2", "id2", "property2");
    consensus.setKey(newKey);
    assertEquals(newKey, consensus.getKey());

    assertThrows(IllegalArgumentException.class, () -> consensus.setKey(null));
  }

  @Test
  public void setAndGetCreationTest() {
    assertEquals(creation, consensus.getCreation());

    long newCreation = creation + 10;
    consensus.setCreation(newCreation);
    assertEquals(newCreation, consensus.getCreation());
  }

  @Test
  public void setAndGetProposerTest() {
    String proposer = "aaa";
    consensus.setProposer(proposer);
    assertEquals(proposer, consensus.getProposer());

    assertThrows(IllegalArgumentException.class, () -> consensus.setProposer(null));
  }

  @Test
  public void setAndGetAcceptorsTest() {
    Set<String> acceptors = new HashSet<>();
    consensus.setAcceptors(acceptors);
    assertEquals(acceptors, consensus.getAcceptors());

    assertThrows(IllegalArgumentException.class, () -> consensus.setAcceptors(null));
  }

  @Test
  public void acceptorsResponsesTest() {
    String acceptor1 = "aaa1";
    String acceptor2 = "aaa2";
    String messageId1 = "mmm1";
    String messageId2 = "mmm2";
    consensus.putAcceptorResponse(acceptor1, messageId1, true);
    consensus.putAcceptorResponse(acceptor2, messageId2, false);

    Map<String, Boolean> responses = consensus.getAcceptorsResponses();
    assertEquals(2, responses.size());
    assertTrue(responses.get(acceptor1));
    assertFalse(responses.get(acceptor2));

    Map<String, String> messageIds = consensus.getAcceptorsToMessageId();
    assertEquals(2, messageIds.size());
    assertEquals(messageId1, messageIds.get(acceptor1));
    assertEquals(messageId2, messageIds.get(acceptor2));

    assertThrows(
        IllegalArgumentException.class, () -> consensus.putAcceptorResponse(acceptor1, null, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> consensus.putAcceptorResponse(null, messageId1, true));
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
  public void generateConsensusIdTest() {
    // Hash("consensus"||created_at||key:type||key:id||key:property||value)
    String expectedId =
        Hash.hash(
            "consensus", Long.toString(creation), type, objId, property, String.valueOf(value));
    assertEquals(expectedId, Consensus.generateConsensusId(creation, type, objId, property, value));
  }
}
