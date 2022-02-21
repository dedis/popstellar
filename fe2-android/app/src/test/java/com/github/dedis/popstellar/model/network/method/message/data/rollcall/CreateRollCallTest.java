package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;

public class CreateRollCallTest {

  private final String laoId = Hash.hash("laoId");
  private final String name = "name";
  private final long now = Instant.now().getEpochSecond();
  private final long end = now + 30L;
  private final String location = "Location";
  private final CreateRollCall createRollCall =
      new CreateRollCall(name, now, now, end, location, null, laoId);

  @Test
  public void generateCreateRollCallIdTest() {
    // Hash('R'||lao_id||creation||name)
    String expectedId =
        Hash.hash(
            EventType.ROLL_CALL.getSuffix(),
            laoId,
            Long.toString(createRollCall.getCreation()),
            createRollCall.getName());
    assertThat(createRollCall.getId(), is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(createRollCall.getObject(), is(Objects.ROLL_CALL.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(createRollCall.getAction(), is(Action.CREATE.getAction()));
  }

  @Test
  public void getNameTest() {
    assertThat(createRollCall.getName(), is(name));
  }

  @Test
  public void getProposedStartTest() {
    assertThat(createRollCall.getProposedStart(), is(now));
  }

  @Test
  public void getProposedEndTest() {
    assertThat(createRollCall.getProposedEnd(), is(end));
  }

  @Test
  public void getDescriptionTest() {
    assertThat(createRollCall.getLocation(), is(location));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(createRollCall);
  }
}
