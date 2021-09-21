package com.github.dedis.popstellar.model.network.method;

public class QueryTest {

  //  private final Keys sender = new Keys();
  //  private final String data = "data";
  //  private final String signature = Signature.sign(sender.getPrivateKey(), data);
  //  private final MessageGeneral message =
  //      new MessageGeneral(
  //          sender.getPublicKey(), data, signature, Hash.hash(data, signature), new
  // ArrayList<>());
  //  private final Subscribe subscribe = new Subscribe("channel", 0);
  //  private final Unsubscribe unsubscribe = new Unsubscribe("channel", 0);
  //  private final Catchup catchup = new Catchup("channel", 0);
  //  private final Publish publish = new Publish("channel", 0, message);
  //
  //  @Test
  //  public void createQueryWithNullParameters() {
  //    assertThrows(IllegalArgumentException.class, () -> new Subscribe(null, 0));
  //    assertThrows(IllegalArgumentException.class, () -> new Unsubscribe(null, 0));
  //    assertThrows(IllegalArgumentException.class, () -> new Catchup(null, 0));
  //    assertThrows(IllegalArgumentException.class, () -> new Publish("channel", 0, null));
  //    assertThrows(IllegalArgumentException.class, () -> new Publish(null, 0, message));
  //  }
  //
  //  @Test
  //  public void getRequestIdTest() {
  //    assertThat(subscribe.getRequestId(), is(0));
  //    assertThat(unsubscribe.getRequestId(), is(0));
  //    assertThat(catchup.getRequestId(), is(0));
  //    assertThat(publish.getRequestId(), is(0));
  //  }
  //
  //  @Test
  //  public void getMessageTest() {
  //    assertThat(publish.getMessage(), is(message));
  //  }
  //
  //  @Test
  //  public void getMethodTest() {
  //    assertThat(subscribe.getMethod(), is(Method.SUBSCRIBE.getMethod()));
  //    assertThat(unsubscribe.getMethod(), is(Method.UNSUBSCRIBE.getMethod()));
  //    assertThat(catchup.getMethod(), is(Method.CATCHUP.getMethod()));
  //    assertThat(publish.getMethod(), is(Method.PUBLISH.getMethod()));
  //  }
  //
  //  @Test
  //  public void equalsTest() {
  //    assertEquals(subscribe, subscribe);
  //    assertNotEquals(subscribe, new Subscribe("channel", 1));
  //
  //    assertEquals(unsubscribe, unsubscribe);
  //    assertNotEquals(unsubscribe, new Unsubscribe("channel", 1));
  //
  //    assertEquals(catchup, catchup);
  //    assertNotEquals(catchup, new Catchup("channel", 1));
  //
  //    assertEquals(publish, publish);
  //    assertNotEquals(publish, new Publish("channel", 1, message));
  //  }
  //
  //  @Test
  //  public void hashCodeTest() {
  //    assertEquals(subscribe.hashCode(), subscribe.hashCode());
  //    assertNotEquals(subscribe.hashCode(), (new Subscribe("channel", 2)).hashCode());
  //
  //    assertEquals(unsubscribe.hashCode(), unsubscribe.hashCode());
  //    assertNotEquals(unsubscribe.hashCode(), (new Subscribe("channel", 2)).hashCode());
  //
  //    assertEquals(catchup.hashCode(), catchup.hashCode());
  //    assertNotEquals(catchup.hashCode(), (new Catchup("channel", 2)).hashCode());
  //
  //    assertEquals(publish.hashCode(), publish.hashCode());
  //    assertNotEquals(publish.hashCode(), (new Publish("channel", 2, message)).hashCode());
  //  }
}
