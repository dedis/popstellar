package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ConsensusKeyTest {

  private static final String type = "TestType";
  private static final String id = Hash.hash("TestId");
  private static final String property = "TestProperty";

  private static final ConsensusKey key = new ConsensusKey(type, id, property);

  @Test
  public void getTypeTest() {
    assertEquals(type, key.getType());
  }

  @Test
  public void getIdTest() {
    assertEquals(id, key.getId());
  }

  @Test
  public void getPropertyTest() {
    assertEquals(property, key.getProperty());
  }

  @Test
  public void equalsTest() {
    assertEquals(key, new ConsensusKey(type, id, property));

    String random = "random";
    assertNotEquals(key, new ConsensusKey(random, id, property));
    assertNotEquals(key, new ConsensusKey(type, random, property));
    assertNotEquals(key, new ConsensusKey(type, id, random));
  }
}
