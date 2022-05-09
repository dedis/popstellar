package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import org.junit.Test;

public class KeyElectionTest {

  public static String ELEC_ID1 = "1";
  public static String ELEC_ID2 = "2";
  public static String KEY1 = "KEY_1";
  public static String KEY2 = "KEY_2";
  public static KeyElection ELEC_KEY1 = new KeyElection(ELEC_ID1, KEY1);

  @Test
  public void getElectionIdTest() {
    assertEquals(ELEC_ID1, ELEC_KEY1.getElectionId());
  }

  @Test
  public void getElectionKetTest(){
    assertEquals(KEY1, ELEC_KEY1.getElectionKey());
  }

  @Test
  public void getActionTest() {
    assertEquals(Action.KEY.getAction(), ELEC_KEY1.getAction());
  }

  @Test
  public void getObjectTest() {
    assertEquals(Objects.ELECTION.getObject(),ELEC_KEY1 .getObject());
  }

  @Test
  public void equalsTest() {
    KeyElection ELEC_KEY2 = new KeyElection(ELEC_ID2, KEY2);
    assertNotEquals(ELEC_KEY1,ELEC_KEY2);

    KeyElection testElect1 = new KeyElection(ELEC_ID1, KEY1);
    assertEquals(testElect1,ELEC_KEY1);
    assertEquals(ELEC_KEY1, ELEC_KEY1);
    assertNotEquals(ELEC_KEY1, null);
    assertEquals(testElect1.hashCode(), ELEC_KEY1.hashCode());
  }

  @Test
  public void testToString() {
    String testFormat =
        "KeyElection{election='1', election_key='KEY_1'}";
    assertEquals(testFormat, ELEC_KEY1.toString());
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(ELEC_KEY1);
  }

}