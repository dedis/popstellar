package com.github.dedis.popstellar.utility.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.github.dedis.popstellar.model.RollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.utility.network.IdGenerator;
import org.junit.Assert;
import org.junit.Test;

public class HashTest {

  @Test
  public void hashTest() {
    assertNotNull(Hash.hash("Data to hash"));
  }

  @Test
  public void hashNullTest() {
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash());
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash((String) null));
    Assert.assertThrows(
        IllegalArgumentException.class, () -> Hash.hash((String) null, (String) null));
  }

  @Test
  public void hashEmptyStringTest() {
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash(""));
    Assert.assertThrows(IllegalArgumentException.class, () -> Hash.hash("", ""));
  }

  @Test
  public void hashObjectTest() {
    // Hashing : CreateRollCall ID from past pop party
    String id = IdGenerator.generateCreateRollCallId(
        "u_y6BWJaedUb8C7xY2V9P1SC2ocaQkMymQgCX2SZGPo=",
        1631871775,
        "mon rôle call"
    );
    String expected = "axL39-AXOH9nJnLEueyNI6Q-zbmZNSfOq5WOJSB8nyc=";
    assertEquals(expected, id);
  }
}
