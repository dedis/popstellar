package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import org.junit.Test;

public class ConsensusElectTest {

  private final long time = Instant.now().getEpochSecond();

  private final String objId = Hash.hash("test");
  private final String type = "TestType";
  private final String property = "TestProperty";
  private final Object value = "TestValue";

  private final ConsensusKey key = new ConsensusKey(type, objId, property);

  private final ConsensusElect consensusElect =
      new ConsensusElect(time, objId, type, property, value);

  @Test
  public void getInstanceIdTest() {
    // Hash("consensus"||created_at||key:type||key:id||key:property||value)
    String expectedId =
        Hash.hash("consensus", Long.toString(time), type, objId, property, String.valueOf(value));
    assertEquals(expectedId, consensusElect.getInstanceId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(time, consensusElect.getCreation());
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
    assertEquals(consensusElect, new ConsensusElect(time, objId, type, property, value));

    assertNotEquals(consensusElect, new ConsensusElect(time + 1, objId, type, property, value));
    assertNotEquals(consensusElect, new ConsensusElect(time, "random", type, property, value));
    assertNotEquals(consensusElect, new ConsensusElect(time, objId, "random", property, value));
    assertNotEquals(consensusElect, new ConsensusElect(time, objId, type, "random", value));
    assertNotEquals(consensusElect, new ConsensusElect(time, objId, type, property, "random"));
  }
}
