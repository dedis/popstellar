package com.github.dedis.popstellar.model.network.method.message.data.lao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GreetLaoTest {

  public static final String LAO_ID = "someID";
  public static final String RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8";
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";
  public static final String RANDOM_PEER = "http://128.0.0.2";

  public static GreetLao GREETING_MSG =
      new GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER);

  @Test(expected = IllegalArgumentException.class)
  public void noInstantiationWithWronsgPublicKey() {
    new GreetLao(LAO_ID, "IsNotValid", RANDOM_ADDRESS, "");
  }

  @Test
  public void getActionTest(){
    assertEquals(Action.GREET.getAction(), GREETING_MSG.getAction());
  }

  @Test
  public void getObjectTest(){
    assertEquals(Objects.LAO.getObject(), GREETING_MSG.getObject());
  }

  @Test
  public void getFrontendKeyTest(){
    assertEquals(RANDOM_KEY, GREETING_MSG.getFrontendKey());
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
    GreetLao GREETING_MSG_2 = new GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER);

    assertNotEquals(GREETING_MSG, new GreetLao("some_id2", RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER));
    assertEquals(GREETING_MSG, GREETING_MSG_2);
    assertNotEquals(GREETING_MSG, null);
    assertEquals(GREETING_MSG_2.hashCode(), GREETING_MSG.hashCode());
    assertEquals(GREETING_MSG, GREETING_MSG);
    assertNotEquals(GREETING_MSG,
        new GreetLao(LAO_ID, RANDOM_KEY, "123", RANDOM_PEER));
    assertNotEquals(GREETING_MSG,
        new GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, "123"));
    assertNotEquals(
        GREETING_MSG,
        new GreetLao(
            LAO_ID, "TrWJNl4kA9VUBydvUwfWw9A-EJlLL6xLaQqRdynvhYw", RANDOM_ADDRESS, "123"));
  }

  @Test
  public void toStringTest(){
    List<String> listTest = Collections.singletonList(RANDOM_PEER);
    String greetingToString =
        String.format(
            "GreetLao={lao='%s', " + "frontend='%s', " + "address='%s', " + "peers='%s'}",
            LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, Arrays.toString(listTest.toArray()));
    assertEquals(greetingToString, GREETING_MSG.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(GREETING_MSG);
  }
}