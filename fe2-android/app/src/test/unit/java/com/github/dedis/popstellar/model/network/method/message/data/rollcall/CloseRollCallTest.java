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
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CloseRollCallTest {

  private static final String LAO_ID = Hash.hash("LAO_ID");
  private static final String NAME = "NAME";
  private static final long TIME = Instant.now().getEpochSecond();
  private static final String LOCATION = "Location";
  private static final CreateRollCall CREATE_ROLL_CALL =
      new CreateRollCall(NAME, TIME, TIME, TIME, LOCATION, null, LAO_ID);
  private static final OpenRollCall OPEN_ROLL_CALL =
      new OpenRollCall(LAO_ID, CREATE_ROLL_CALL.id, TIME, EventState.CREATED);
  private static final CloseRollCall CLOSE_ROLL_CALL =
      new CloseRollCall(LAO_ID, OPEN_ROLL_CALL.updateId, TIME, new ArrayList<>());

  @Test
  public void generateCloseRollCallIdTest() {
    // Hash('R'||lao_id||closes||closed_at)
    String expectedId =
        Hash.hash(
            EventType.ROLL_CALL.suffix,
            LAO_ID,
            CLOSE_ROLL_CALL.closes,
            Long.toString(CLOSE_ROLL_CALL.closedAt));
    assertThat(CLOSE_ROLL_CALL.updateId, is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(CLOSE_ROLL_CALL.getObject(), is(Objects.ROLL_CALL.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(CLOSE_ROLL_CALL.getAction(), is(Action.CLOSE.getAction()));
  }

  @Test
  public void getAttendeesListTest() {
    assertThat(CLOSE_ROLL_CALL.getAttendees(), is(new ArrayList<>()));
  }

  @Test
  public void getClosedAtTest() {
    assertThat(CLOSE_ROLL_CALL.closedAt, is(TIME));
  }

  @Test
  public void getClosesTest() {
    assertThat(CLOSE_ROLL_CALL.closes, is(OPEN_ROLL_CALL.updateId));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(CLOSE_ROLL_CALL);
  }

  @Test
  public void equalsTest() {
    assertEquals(CLOSE_ROLL_CALL, CLOSE_ROLL_CALL);
    assertNotEquals(null, CLOSE_ROLL_CALL);
    CloseRollCall closeRollCall =
        new CloseRollCall(LAO_ID, OPEN_ROLL_CALL.updateId, TIME, new ArrayList<>());
    assertEquals(CLOSE_ROLL_CALL, closeRollCall);
  }

  @Test
  public void hashCodeTest() {
    String updateId =
        Hash.hash(
            EventType.ROLL_CALL.suffix, LAO_ID, OPEN_ROLL_CALL.updateId, Long.toString(TIME));
    assertEquals(
        java.util.Objects.hash(updateId, OPEN_ROLL_CALL.updateId, TIME, new ArrayList<>()),
        CLOSE_ROLL_CALL.hashCode());
  }
}
