package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ElectionKeyTest {

  public static String ELEC_ID1 = Base64DataUtils.generateRandomBase64String();
  public static String ELEC_ID2 = Base64DataUtils.generateRandomBase64String();

  public static String KEY1 = Base64DataUtils.generateRandomBase64String();

  public static String KEY2 = Base64DataUtils.generateRandomBase64String();

  public static ElectionKey ELECTION_KEY1 = new ElectionKey(ELEC_ID1, KEY1);
  public static ElectionKey ELECTION_KEY2 = new ElectionKey(ELEC_ID2, KEY2);

  @Before
  public void setup() {
    JsonTestUtils.loadGSON(ApplicationProvider.getApplicationContext());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsElectionIdNotBase64Test() {
    new ElectionKey("not base 64", KEY1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsElectionVoteKeyNotBase64Test() {
    new ElectionKey(ELEC_ID1, "not base 64");
  }

  @Test
  public void getElectionIdTest() {
    assertEquals(ELEC_ID1, ELECTION_KEY1.getElectionId());
  }

  @Test
  public void getElectionVoteKeyTest() {
    assertEquals(KEY1, ELECTION_KEY1.getElectionVoteKey());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.KEY.getAction(), ELECTION_KEY1.getAction());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.ELECTION.getObject(), ELECTION_KEY1.getObject());
  }

  @Test
  public void equalsTest() {
    assertNotEquals(ELECTION_KEY1, ELECTION_KEY2);

    ElectionKey testElect1 = new ElectionKey(ELEC_ID1, KEY1);
    assertEquals(testElect1, ELECTION_KEY1);
    assertNotEquals(ELECTION_KEY1, null);
    assertEquals(testElect1.hashCode(), ELECTION_KEY1.hashCode());
  }

  @Test
  public void testToString() {
    String testFormat = "ElectionKey{election='" + ELEC_ID1 + "', election_key='" + KEY1 + "'}";
    assertEquals(testFormat, ELECTION_KEY1.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(ELECTION_KEY1);
    String pathDir = "protocol/examples/messageData/election_key/";
    String valid1 = JsonTestUtils.loadFile(pathDir + "election_key.json");
    JsonTestUtils.parse(valid1);

    // Check that invalid data is rejected
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_election_key_additional_property.json");
    String jsonInvalid2 =
        JsonTestUtils.loadFile(pathDir + "wrong_election_key_missing_action.json");
    String jsonInvalid3 =
        JsonTestUtils.loadFile(pathDir + "wrong_election_key_missing_election.json");
    String jsonInvalid4 =
        JsonTestUtils.loadFile(pathDir + "wrong_election_key_missing_election_key.json");
    String jsonInvalid5 =
        JsonTestUtils.loadFile(pathDir + "wrong_election_key_missing_object.json");

    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid3));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid4));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid5));
  }
}
