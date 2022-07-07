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

public class PublishTest {
  private static final Channel CHANNEL = Channel.fromString("root/stuff");
  private static final int ID = 42;
  private static final CreateRollCall DATA =
      new CreateRollCall("title", 0, 1, 2, "EPFL", "rc", "an id");
  private static final KeyPair KEYPAIR = Base64DataUtils.generateKeyPair();
  private static final MessageGeneral MESSAGE_GENERAL =
      new MessageGeneral(KEYPAIR, DATA, new Gson());
  private static final Publish PUBLISH = new Publish(CHANNEL, ID, MESSAGE_GENERAL);

  @Test
  public void getMethod() {
    assertEquals("publish", PUBLISH.getMethod());
  }

  @Test
  public void testEquals() {
    assertEquals(PUBLISH, PUBLISH);
    assertNotEquals(null, PUBLISH);

    Publish publish2 = new Publish(CHANNEL, 1, MESSAGE_GENERAL);
    assertNotEquals(PUBLISH, publish2);

    Publish publish3 = new Publish(CHANNEL, ID, MESSAGE_GENERAL);
    assertEquals(PUBLISH, publish3);
  }

  @Test
  public void testHashCode() {
    assertEquals(
        Objects.hash(Objects.hash(Objects.hash(CHANNEL), ID), MESSAGE_GENERAL), PUBLISH.hashCode());
  }

  @Test
  public void testToString() {
    String expected =
        String.format(
            "Publish{id=%s, channel='%s'," + " message=%s}", ID, CHANNEL, MESSAGE_GENERAL);
    assertEquals(expected, PUBLISH.toString());
  }

  @Test
  public void emptyMsgGeneralInConstructorThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> new Publish(CHANNEL, ID, null));
  }
}
