package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.PublicKey;
import org.junit.Test;

public class ServerTest {

  public static final PublicKey RANDOM_KEY =
      new PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8");
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";

  public static final Server SERVER = new Server(RANDOM_ADDRESS, RANDOM_KEY);

  @Test
  public void getPublicKeyTest() {
    assertEquals(RANDOM_KEY, SERVER.getPublicKey());
  }

  @Test
  public void getServerAddressTest() {
    assertEquals(RANDOM_ADDRESS, SERVER.getServerAddress());
  }
}
