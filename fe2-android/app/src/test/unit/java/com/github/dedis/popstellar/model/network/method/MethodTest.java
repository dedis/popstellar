package com.github.dedis.popstellar.model.network.method;

import org.junit.Test;

import static org.junit.Assert.*;

public class MethodTest {

  @Test
  public void getDataClassTest() {
    assertEquals(Subscribe.class, Method.SUBSCRIBE.dataClass);
    assertEquals(Unsubscribe.class, Method.UNSUBSCRIBE.dataClass);
    assertEquals(Publish.class, Method.PUBLISH.dataClass);
    assertEquals(Broadcast.class, Method.MESSAGE.dataClass);
    assertEquals(Catchup.class, Method.CATCHUP.dataClass);
  }

  @Test
  public void expectResultTest() {
    assertTrue(Method.SUBSCRIBE.expectResult());
    assertTrue(Method.UNSUBSCRIBE.expectResult());
    assertTrue(Method.PUBLISH.expectResult());
    assertFalse(Method.MESSAGE.expectResult());
    assertTrue(Method.CATCHUP.expectResult());
  }

  @Test
  public void findTest() {
    assertNull(Method.find("not a method"));
    assertEquals(Method.SUBSCRIBE, Method.find("subscribe"));
  }
}
