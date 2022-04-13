package com.github.dedis.popstellar.repository;

import static org.junit.Assert.*;

import com.github.dedis.popstellar.model.objects.Server;

import org.junit.Before;
import org.junit.Test;

public class ServerRepositoryTest {

  public static ServerRepository serverRepository = new ServerRepository();
  public static String key_1 = "key_1";
  public static String key_2 = "key_2";
  public static String address_1 = "10.0.2.2:9000";
  public static String address_2 = "128.0.0.2:9445";
  public static Server server1 = new Server(address_1, key_1);
  public static Server server2 = new Server(address_2, key_2);

  @Before
  public void setUp(){
    serverRepository.addServer(server1);
    serverRepository.addServer(server2);
  }

  @Test
  public void getServerByURLTest() {
    assertEquals(key_1, serverRepository.getServerByURL(address_1).getPublicKey());
  }

  @Test
  public void getAllServerTest() {
    assertFalse(serverRepository.getAllServer().isEmpty());
  }
}