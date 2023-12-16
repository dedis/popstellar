package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OpenRollCallTest {

  private static final String LAO_ID = Hash.hash("LAO_ID");
  private static final String NAME = "NAME";
  private static final long TIME = Instant.now().getEpochSecond();
  private static final String LOCATION = "Location";
  private static final CreateRollCall CREATE_ROLL_CALL =
      new CreateRollCall(NAME, TIME, TIME, TIME, LOCATION, null, LAO_ID);
  private static final OpenRollCall OPEN_ROLL_CALL =
      new OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CREATED);
  private static final OpenRollCall REOPEN_ROLL_CALL =
      new OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CLOSED);
  private static final String ID =
      Hash.hash(EventType.ROLL_CALL.suffix, LAO_ID, CREATE_ROLL_CALL.id, Long.toString(TIME));

  @Test
  public void generateOpenRollCallIdTest() {
    // Hash('R'||lao_id||opens||opened_at)
    String expectedId =
        Hash.hash(
            EventType.ROLL_CALL.suffix,
            LAO_ID,
            REOPEN_ROLL_CALL.opens,
            Long.toString(REOPEN_ROLL_CALL.openedAt));
    assertThat(REOPEN_ROLL_CALL.updateId, is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(REOPEN_ROLL_CALL.getObject(), is(Objects.ROLL_CALL.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(OPEN_ROLL_CALL.action, is(Action.OPEN.getAction()));
    assertThat(REOPEN_ROLL_CALL.action, is(Action.REOPEN.getAction()));
  }

  @Test
  public void getOpenedAtTest() {
    assertThat(REOPEN_ROLL_CALL.openedAt, is(TIME));
  }

  @Test
  public void getOpensTest() {
    assertThat(REOPEN_ROLL_CALL.opens, is(CREATE_ROLL_CALL.id));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(REOPEN_ROLL_CALL);
  }

  @Test
  public void equalsTest() {
    assertEquals(OPEN_ROLL_CALL, OPEN_ROLL_CALL);
    assertNotEquals(null, OPEN_ROLL_CALL);

    OpenRollCall openRollCall =
        new OpenRollCall(ID, CREATE_ROLL_CALL.id, TIME, Action.OPEN.getAction());
    assertEquals(OPEN_ROLL_CALL, openRollCall);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(
        java.util.Objects.hash(ID, CREATE_ROLL_CALL.id, TIME, Action.OPEN.getAction()),
        OPEN_ROLL_CALL.hashCode());
  }
}
