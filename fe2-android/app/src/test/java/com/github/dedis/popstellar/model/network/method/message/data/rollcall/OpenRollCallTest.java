package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;

public class OpenRollCallTest {

  private final String laoId = Hash.hash("laoId");
  private final String name = "name";
  private final long time = Instant.now().getEpochSecond();
  private final String location = "Location";
  private final CreateRollCall createRollCall =
      new CreateRollCall(name, time, time, time, location, null, laoId);
  private final OpenRollCall openRollCall =
      new OpenRollCall(laoId, createRollCall.getId(), time, EventState.CREATED);
  private final OpenRollCall reopenRollCall =
      new OpenRollCall(laoId, createRollCall.getId(), time, EventState.CLOSED);

  @Test
  public void generateOpenRollCallIdTest() {
    // Hash('R'||lao_id||opens||opened_at)
    String expectedId =
        Hash.hash(
            EventType.ROLL_CALL.getSuffix(),
            laoId,
            reopenRollCall.getOpens(),
            Long.toString(reopenRollCall.getOpenedAt()));
    assertThat(reopenRollCall.getUpdateId(), is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(reopenRollCall.getObject(), is(Objects.ROLL_CALL.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(openRollCall.getAction(), is(Action.OPEN.getAction()));
    assertThat(reopenRollCall.getAction(), is(Action.REOPEN.getAction()));
  }

  @Test
  public void getOpenedAtTest() {
    assertThat(reopenRollCall.getOpenedAt(), is(time));
  }

  @Test
  public void getOpensTest() {
    assertThat(reopenRollCall.getOpens(), is(createRollCall.getId()));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(reopenRollCall);
  }
}
