package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.Server;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServerRepositoryTest {

  public static ServerRepository serverRepository = new ServerRepository();
  public static String LAO_1 = "id_1";
  public static String LAO_2 = "id_2";
  public static PublicKey KEY_1 = new PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8");
  public static PublicKey KEY_2 = new PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm9");
  public static String ADDRESS_1 = "10.0.2.2:9000";
  public static String ADDRESS_2 = "128.0.0.2:9445";
  public static Server SERVER_1 = new Server(ADDRESS_1, KEY_1);
  public static Server SERVER_2 = new Server(ADDRESS_2, KEY_2);

  @Before
  public void setUp() {
    serverRepository.addServer(LAO_1, SERVER_1);
    serverRepository.addServer(LAO_2, SERVER_2);
  }

  @Test
  public void getServerByLaoId() {
    assertEquals(SERVER_1, serverRepository.getServerByLaoId(LAO_1));
  }

  @Test
  public void getServerByLaoIdFailsWithEmptyRepo() {
    ServerRepository emptyRepo = new ServerRepository();
    assertThrows(IllegalArgumentException.class, () -> emptyRepo.getServerByLaoId(LAO_1));
  }

  @Test
  public void getAllServerTest() {
    assertFalse(serverRepository.getAllServer().isEmpty());
  }
}
