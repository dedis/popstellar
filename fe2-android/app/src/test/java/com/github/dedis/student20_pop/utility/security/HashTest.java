package com.github.dedis.student20_pop.utility.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class HashTest {

  @Test
  public void hashNullDataTest() {
    assertThrows(IllegalArgumentException.class, () -> Hash.hash((String) null));
    assertThrows(IllegalArgumentException.class, () -> Hash.hash(null, null));
  }

  @Test
  public void hashTest() {
    assertNotNull(Hash.hash("Data to hash"));
  }

  @Test
  public void hashObjectTest() {
    // Hashing : test 0 \fwa"fwa-fwa
    String expected = Hash.hash("[\"test\",\"0\",\"\\\\fwa\\\"fwa-fwa\"]");
    //    assertThat(Hash.hash("test", 0, "\\fwa\"fwa-fwa"), is(expected));
  }
}
