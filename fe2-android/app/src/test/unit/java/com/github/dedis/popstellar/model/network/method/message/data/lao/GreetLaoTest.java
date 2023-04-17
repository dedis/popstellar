package com.github.dedis.popstellar.model.network.method.message.data.lao;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.JsonParseException;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class GreetLaoTest {

  public static final String LAO_ID = "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=";
  public static final String RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8";
  public static final String RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client";

  public static final PeerAddress RANDOM_PEER = new PeerAddress("ws://10.0.1.1:7000/");
  public static List<PeerAddress> RANDOM_PEER_LIST = new ArrayList<>(Arrays.asList(RANDOM_PEER));

  public static GreetLao GREETING_MSG =
      new GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER_LIST);

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsWrongPublicKeyTest() {
    new GreetLao(LAO_ID, "IsNotValid", RANDOM_ADDRESS, Collections.emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsLaoIdNotBase64Test() {
    new GreetLao("wrong id", RANDOM_KEY, RANDOM_ADDRESS, Collections.emptyList());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.GREET.getAction(), GREETING_MSG.getAction());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.LAO.getObject(), GREETING_MSG.getObject());
  }

  @Test
  public void getFrontendKeyTest() {
    assertEquals(new PublicKey(RANDOM_KEY), GREETING_MSG.getFrontendKey());
  }

  @Test
  public void getAddressTest() {
    assertEquals(RANDOM_ADDRESS, GREETING_MSG.getAddress());
  }

  @Test
  public void getPeersTest() {
    List<PeerAddress> peersList = new ArrayList<>(RANDOM_PEER_LIST);
    assertEquals(GREETING_MSG.getPeers(), peersList);
  }

  @Test
  public void equalsTest() {
    GreetLao GREETING_MSG_2 = new GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER_LIST);
    PeerAddress RANDOM_PEER_2 = new PeerAddress("123");

    assertNotEquals(
        GREETING_MSG, new GreetLao("some_id2", RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER_LIST));
    assertEquals(GREETING_MSG, GREETING_MSG_2);
    assertNotEquals(GREETING_MSG, null);
    assertEquals(GREETING_MSG_2.hashCode(), GREETING_MSG.hashCode());
    assertEquals(GREETING_MSG, GREETING_MSG);
    assertNotEquals(GREETING_MSG, new GreetLao(LAO_ID, RANDOM_KEY, "123", RANDOM_PEER_LIST));
    assertNotEquals(
        GREETING_MSG,
        new GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, Arrays.asList(RANDOM_PEER_2)));
    assertNotEquals(
        GREETING_MSG,
        new GreetLao(
            LAO_ID,
            "TrWJNl4kA9VUBydvUwfWw9A-EJlLL6xLaQqRdynvhYw",
            RANDOM_ADDRESS,
            Arrays.asList(RANDOM_PEER_2)));
  }

  @Test
  public void toStringTest() {
    List<PeerAddress> listTest = new ArrayList<>(RANDOM_PEER_LIST);
    String greetingToString =
        String.format(
            "GreetLao={lao='%s', " + "frontend='%s', " + "address='%s', " + "peers=%s}",
            LAO_ID, new PublicKey(RANDOM_KEY), RANDOM_ADDRESS, Arrays.toString(listTest.toArray()));
    assertEquals(greetingToString, GREETING_MSG.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(GREETING_MSG);

    String pathDir = "protocol/examples/messageData/lao_greet/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_greeting_additional_property_0.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_greeting_additional_property_2.json");
    String jsonInvalid3 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_invalid_address_2.json");
    String jsonInvalid4 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_invalid_address.json");
    String jsonInvalid5 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_action.json");
    String jsonInvalid6 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_address_0.json");
    String jsonInvalid7 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_address_1.json");
    String jsonInvalid8 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_frontend.json");
    String jsonInvalid9 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_lao.json");
    String jsonInvalid10 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_object.json");
    String jsonInvalid11 = JsonTestUtils.loadFile(pathDir + "wrong_greeting_missing_peers.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid3));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid4));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid5));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid6));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid7));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid8));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid9));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid10));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid11));
  }
}
