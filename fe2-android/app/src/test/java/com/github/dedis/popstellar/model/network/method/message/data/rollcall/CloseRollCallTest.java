package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.Test;

public class CloseRollCallTest {

  private final String laoId = Hash.hash("laoId");
  private final String name = "name";
  private final long time = Instant.now().getEpochSecond();
  private final String location = "Location";
  private final CreateRollCall createRollCall = new CreateRollCall(name, time, time, time, location, null, laoId);
  private final OpenRollCall openRollCall = new OpenRollCall(laoId, createRollCall.getId(), time, EventState.CREATED);
  private final CloseRollCall closeRollCall = new CloseRollCall(laoId, openRollCall.getUpdateId(), time, new ArrayList<>());

  @Test
  public void generateCloseRollCallIdTest() {
    // Hash('R'||lao_id||closes||closed_at)
    String expectedId = Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, closeRollCall.getCloses(),
        Long.toString(closeRollCall.getClosedAt()));
    assertThat(closeRollCall.getUpdateId(), is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(closeRollCall.getObject(), is(Objects.ROLL_CALL.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(closeRollCall.getAction(), is(Action.CLOSE.getAction()));
  }

  @Test
  public void getAttendeesListTest() {
    assertThat(closeRollCall.getAttendees(), is(new ArrayList<>()));
  }

  @Test
  public void getClosedAtTest() {
    assertThat(closeRollCall.getClosedAt(), is(time));
  }

  @Test
  public void getClosesTest() {
    assertThat(closeRollCall.getCloses(), is(openRollCall.getUpdateId()));
  }
}
