package com.github.dedis.popstellar.repository.local;

import com.github.dedis.popstellar.model.objects.Channel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PersistentDataTest {

  @Test
  public void persistentDataGettersAreConsistentWithConstructor() {
    String[] seed = new String[] {"foo", "bar", "foobar"};
    String address = "deadBeef";
    Set<Channel> channels = new HashSet<>(Collections.singletonList(Channel.ROOT));
    PersistentData persistentData = new PersistentData(seed, address, channels);

    assertArrayEquals(seed, persistentData.getWalletSeed());
    assertEquals(address, persistentData.getServerAddress());
    assertEquals(channels, persistentData.getSubscriptions());
  }
}
