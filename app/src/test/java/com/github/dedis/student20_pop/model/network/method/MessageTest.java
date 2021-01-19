package com.github.dedis.student20_pop.model.network.method;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MessageTest {

  private final Keys sender = new Keys();
  private final String data = "data";
  private final String signature = Signature.sign(sender.getPrivateKey(), data);
  private final MessageGeneral message =
      new MessageGeneral(
          sender.getPublicKey(), data, signature, Hash.hash(data, signature), new ArrayList<>());
  private final Broadcast broadcast1 = new Broadcast("channel1", message);
  private final Broadcast broadcast2 = new Broadcast("channel2", message);

  @Test
  public void createBroadcastWithNullParametersTest() {
    assertThrows(IllegalArgumentException.class, () -> new Broadcast(null, message));
    assertThrows(IllegalArgumentException.class, () -> new Broadcast("channel2", null));
  }

  @Test
  public void getMessageTest() {
    assertThat(broadcast1.getMessage(), is(message));
  }

  @Test
  public void getMethodTest() {
    assertThat(broadcast1.getMethod(), is(Method.MESSAGE.getMethod()));
  }

  @Test
  public void equalsTest() {
    assertEquals(broadcast1, broadcast1);
    assertNotEquals(broadcast2, broadcast1);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(broadcast1.hashCode(), broadcast1.hashCode());
    assertNotEquals(broadcast1.hashCode(), broadcast2.hashCode());
  }
}
