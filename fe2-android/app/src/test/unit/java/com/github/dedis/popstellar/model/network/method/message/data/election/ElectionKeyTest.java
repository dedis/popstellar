package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ElectionKeyTest {

  public static String ELEC_ID1 = "1";
  public static String ELEC_ID2 = "2";
  public static String KEY1 = "KEY_1";
  public static String KEY2 = "KEY_2";
  public static ElectionKey ELEC_KEY1 = new ElectionKey(ELEC_ID1, KEY1);

  @Test
  public void getElectionIdTest() {
    assertEquals(ELEC_ID1, ELEC_KEY1.getElectionId());
  }

  @Test
  public void getElectionVoteKeyTest() {
    assertEquals(KEY1, ELEC_KEY1.getElectionVoteKey());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.KEY.getAction(), ELEC_KEY1.getAction());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.ELECTION.getObject(), ELEC_KEY1.getObject());
  }

  @Test
  public void equalsTest() {
    ElectionKey ELEC_KEY2 = new ElectionKey(ELEC_ID2, KEY2);
    assertNotEquals(ELEC_KEY1, ELEC_KEY2);

    ElectionKey testElect1 = new ElectionKey(ELEC_ID1, KEY1);
    assertEquals(testElect1, ELEC_KEY1);
    assertNotEquals(ELEC_KEY1, null);
    assertEquals(testElect1.hashCode(), ELEC_KEY1.hashCode());
  }

  @Test
  public void testToString() {
    String testFormat = "ElectionKey{election='1', election_key='KEY_1'}";
    assertEquals(testFormat, ELEC_KEY1.toString());
  }

  @Test
  public void jsonValidationTest() {

    ElectionKey real =
        new ElectionKey(
            "zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=",
            "JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ");

    JsonTestUtils.testData(real);
  }
}
