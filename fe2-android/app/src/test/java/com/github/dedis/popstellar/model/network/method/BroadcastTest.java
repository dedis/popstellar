package com.github.dedis.popstellar.model.network.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Objects;

public class BroadcastTest {
  private static final Channel CHANNEL = Channel.fromString("root/stuff");
  private static final int ID = 42;
  private static final CreateRollCall DATA =
      new CreateRollCall("title", 0, 1, 2, "EPFL", "rc", "an id");
  private static final KeyPair KEYPAIR = Base64DataUtils.generateKeyPair();
  private static final MessageGeneral MESSAGE_GENERAL =
      new MessageGeneral(KEYPAIR, DATA, new Gson());
  private static final Broadcast BROADCAST = new Broadcast(CHANNEL, MESSAGE_GENERAL);

  @Test
  public void getMethod() {
    assertEquals("broadcast", BROADCAST.getMethod());
  }

  @Test
  public void getMessage() {
    assertEquals(MESSAGE_GENERAL, BROADCAST.getMessage());
  }

  @Test
  public void testEquals() {
    assertEquals(BROADCAST, BROADCAST);
    assertNotEquals(null, BROADCAST);

    Broadcast broadcast2 =
        new Broadcast(Channel.fromString("foo/bar/refoo/rebar"), MESSAGE_GENERAL);
    assertNotEquals(broadcast2, BROADCAST);

    Broadcast broadcast3 = new Broadcast(CHANNEL, MESSAGE_GENERAL);
    assertEquals(BROADCAST, broadcast3);
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hash(Objects.hash(CHANNEL), MESSAGE_GENERAL), BROADCAST.hashCode());
  }

  @Test
  public void testToString() {
    String expected = String.format("Broadcast{channel='%s', method='%s'}", CHANNEL, "broadcast");
    assertEquals(expected, BROADCAST.toString());
  }

  @Test
  public void constructorThrowsExceptionForEmptyMessage() {
    assertThrows(IllegalArgumentException.class, () -> new Broadcast(CHANNEL, null));
  }
}
