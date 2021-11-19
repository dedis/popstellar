package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChirpTest {
  private static final String id = "messageid";

  private static final Chirp chirp = new Chirp(id);

  @Test
  public void setAndGetIdTest() {
    String newId = "newmessageid";
    chirp.setId(newId);
    assertEquals(newId, chirp.getId());
  }
}
