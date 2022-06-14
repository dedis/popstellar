package com.github.dedis.popstellar.model.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PeerAddressTest {

  public static String ADDRESS_1 = "ws://122.0.2";
  public static String ADDRESS_2 = "ws://122.0.3";
  public static PeerAddress PEER_ADDRESS_1 = new PeerAddress(ADDRESS_1);
  public static PeerAddress PEER_ADDRESS_2 = new PeerAddress(ADDRESS_2);

  @Test
  public void getAddress() {
    assertEquals(ADDRESS_1, PEER_ADDRESS_1.getAddress());
  }

  @Test
  public void testToString() {
    String address_test = "{address:='" + ADDRESS_1 + "'}";
    assertEquals(address_test, PEER_ADDRESS_1.toString());
  }

  @Test
  public void testEquals() {
    assertEquals(PEER_ADDRESS_1, PEER_ADDRESS_1);
    assertNotEquals(PEER_ADDRESS_1, PEER_ADDRESS_2);
    assertNotEquals(PEER_ADDRESS_1, null);
  }
}
