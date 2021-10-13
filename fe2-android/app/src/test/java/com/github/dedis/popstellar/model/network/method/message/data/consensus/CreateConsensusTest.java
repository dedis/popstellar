package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import org.junit.Test;

public class CreateConsensusTest {

  private final long time = Instant.now().getEpochSecond();

  private final String objId = Hash.hash("test");
  private final String type = "TestType";
  private final String property = "TestProperty";
  private final Object value = "TestValue";

  private final ConsensusKey key = new ConsensusKey(type, objId, property);

  private final CreateConsensus createConsensus =
      new CreateConsensus(time, objId, type, property, value);

  @Test
  public void getInstanceIdTest() {
    // Hash("consensus"||created_at||key:type||key:id||key:property||value)
    String expectedId =
        Hash.hash("consensus", Long.toString(time), type, objId, property, String.valueOf(value));
    assertEquals(expectedId, createConsensus.getInstanceId());
  }

  @Test
  public void getCreationTest() {
    assertEquals(time, createConsensus.getCreation());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.CONSENSUS.getObject(), createConsensus.getObject());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.PHASE_1_ELECT.getAction(), createConsensus.getAction());
  }

  @Test
  public void getKeyTest() {
    assertEquals(key, createConsensus.getKey());
  }

  @Test
  public void getValueTest() {
    assertEquals(value, createConsensus.getValue());
  }

  @Test
  public void equalsTest() {
    assertEquals(createConsensus, new CreateConsensus(time, objId, type, property, value));

    assertNotEquals(createConsensus, new CreateConsensus(time + 1, objId, type, property, value));
    assertNotEquals(createConsensus, new CreateConsensus(time, "random", type, property, value));
    assertNotEquals(createConsensus, new CreateConsensus(time, objId, "random", property, value));
    assertNotEquals(createConsensus, new CreateConsensus(time, objId, type, "random", value));
    assertNotEquals(createConsensus, new CreateConsensus(time, objId, type, property, "random"));
  }
}
