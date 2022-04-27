package com.github.dedis.popstellar.repository;

import static org.junit.Assert.*;

import com.github.dedis.popstellar.model.objects.Server;

import org.junit.Before;
import org.junit.Test;

public class ServerRepositoryTest {

  public static ServerRepository serverRepository = new ServerRepository();
  public static String LAO_1 = "id_1";
  public static String LAO_2 = "id_2";
  public static String KEY_1 = "key_1";
  public static String KEY_2 = "key_2";
  public static String ADDRESS_1 = "10.0.2.2:9000";
  public static String ADDRESS_2 = "128.0.0.2:9445";
  public static Server SERVER_1 = new Server(ADDRESS_1, KEY_1);
  public static Server SERVER_2 = new Server(ADDRESS_2, KEY_2);

  @Before
  public void setUp(){
    serverRepository.addServer(LAO_1, SERVER_1);
    serverRepository.addServer(LAO_2, SERVER_2);
  }

  @Test
  public void getServerByLaoId() {
    assertEquals(SERVER_1, serverRepository.getServerByLaoId(LAO_1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getServerByLaoIdFailsWithEmptyRepo(){
    ServerRepository emptyRepo = new ServerRepository();
    assertEquals(SERVER_1, emptyRepo.getServerByLaoId(LAO_1));
  }

  @Test
  public void getAllServerTest() {
    assertFalse(serverRepository.getAllServer().isEmpty());
  }
}