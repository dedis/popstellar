package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.utility.security.Hash;
import com.google.gson.JsonParseException;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConsensusElectTest {

  private static final long timeInSeconds = 1635277619;

  private static final String objId = Hash.hash("test");
  private static final String type = "TestType";
  private static final String property = "TestProperty";
  private static final Object value = "TestValue";

  private static final ConsensusKey key = new ConsensusKey(type, objId, property);

  private static final ConsensusElect consensusElect =
      new ConsensusElect(timeInSeconds, objId, type, property, value);

  @Test
  public void getInstanceIdTest() {
    // Hash("consensus"||key:type||key:id||key:property)
    String expectedId = Hash.hash("consensus", type, objId, property);
    assertEquals(expectedId, consensusElect.getInstanceId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(timeInSeconds, consensusElect.getCreation());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), consensusElect.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.ELECT.getAction(), consensusElect.getAction());
  }

  @Test
  public void getKeyTest() {
    assertEquals(key, consensusElect.getKey());
  }

  @Test
  public void getValueTest() {
    assertEquals(value, consensusElect.getValue());
  }

  @Test
  public void equalsTest() {
    assertEquals(consensusElect, new ConsensusElect(timeInSeconds, objId, type, property, value));

    String random = "random";
    assertNotEquals(
        consensusElect, new ConsensusElect(timeInSeconds + 1, objId, type, property, value));
    assertNotEquals(
        consensusElect, new ConsensusElect(timeInSeconds, random, type, property, value));
    assertNotEquals(
        consensusElect, new ConsensusElect(timeInSeconds, objId, random, property, value));
    assertNotEquals(consensusElect, new ConsensusElect(timeInSeconds, objId, type, random, value));
    assertNotEquals(
        consensusElect, new ConsensusElect(timeInSeconds, objId, type, property, random));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(consensusElect);
    String jsonInvalid =
        JsonTestUtils.loadFile(
            "protocol/examples/messageData/consensus_elect/wrong_elect_negative_created_at.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid));
  }
}
