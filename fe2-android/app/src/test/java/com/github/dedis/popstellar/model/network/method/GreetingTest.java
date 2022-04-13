package com.github.dedis.popstellar.model.network.method;

import static com.github.dedis.popstellar.model.network.method.Method.GREETING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.objects.Channel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GreetingTest {

  public static final Channel CHANNEL = Channel.ROOT;
  public static final String RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8";
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";
  public static final String RANDOM_PEER = "http://128.0.0.2";

  public static Greeting GREETING_MSG =
      new Greeting(CHANNEL, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER);

  @Test(expected = IllegalArgumentException.class)
  public void noInstantiationWithWronsgPublicKey() {
    new Greeting(CHANNEL, "IsNotValid", RANDOM_ADDRESS, "");
  }

  @Test
  public void getMethodTest(){
    assertEquals(GREETING.getMethod(), GREETING_MSG.getMethod());
  }

  @Test
  public void getSenderKeyTest(){
    assertEquals(RANDOM_KEY, GREETING_MSG.getSenderKey());
  }

  @Test
  public void getAddressTest(){
    assertEquals(RANDOM_ADDRESS, GREETING_MSG.getAddress());
  }

  @Test
  public void getPeersTest(){
    List<String> peersList =
        Collections.singletonList(RANDOM_PEER);
    assertEquals(GREETING_MSG.getPeers(), peersList);
  }

  @Test
  public void equalsTest(){
    Greeting GREETING_MSG_2 = new Greeting(CHANNEL, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER);

    assertEquals(GREETING_MSG, GREETING_MSG_2);
    assertNotEquals(GREETING_MSG, null);
    assertEquals(GREETING_MSG_2.hashCode(), GREETING_MSG.hashCode());
    assertEquals(GREETING_MSG, GREETING_MSG);
    assertNotEquals(GREETING_MSG,
        new Greeting(CHANNEL, RANDOM_KEY, "123", RANDOM_PEER));
    assertNotEquals(GREETING_MSG,
        new Greeting(CHANNEL, RANDOM_KEY, RANDOM_ADDRESS, "123"));
    assertNotEquals(
        GREETING_MSG,
        new Greeting(
            CHANNEL, "TrWJNl4kA9VUBydvUwfWw9A-EJlLL6xLaQqRdynvhYw", RANDOM_ADDRESS, "123"));
  }

  @Test
  public void toStringTest(){
    List<String> listTest = Collections.singletonList(RANDOM_PEER);
    String greetingToString =
        String.format(
            "Greeting={channel='Channel{'/root'}', "
                + "sender='%s', "
                + "address='%s', "
                + "peers='%s'}",
            RANDOM_KEY, RANDOM_ADDRESS, Arrays.toString(listTest.toArray()));
    assertEquals(greetingToString, GREETING_MSG.toString());
  }

}
