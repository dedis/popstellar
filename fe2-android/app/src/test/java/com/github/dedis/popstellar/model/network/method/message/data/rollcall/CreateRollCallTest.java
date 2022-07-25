package com.github.dedis.popstellar.model.network.method.message.data.rollcall;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CreateRollCallTest {

  private static final String LAO_ID = Hash.hash("LAO_ID");
  private static final String NAME = "NAME";
  private static final long NOW = Instant.now().getEpochSecond();
  private static final long END = NOW + 30L;
  private static final String LOCATION = "Location";
  private static final CreateRollCall CREATE_ROLL_CALL =
      new CreateRollCall(NAME, NOW, NOW, END, LOCATION, null, LAO_ID);
  private static final String ID =
      Hash.hash(EventType.ROLL_CALL.getSuffix(), LAO_ID, Long.toString(NOW), NAME);

  @Test
  public void generateCreateRollCallIdTest() {
    // Hash('R'||lao_id||creation||NAME)
    String expectedId =
        Hash.hash(
            EventType.ROLL_CALL.getSuffix(),
            LAO_ID,
            Long.toString(CREATE_ROLL_CALL.getCreation()),
            CREATE_ROLL_CALL.getName());
    assertThat(CREATE_ROLL_CALL.getId(), is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(CREATE_ROLL_CALL.getObject(), is(Objects.ROLL_CALL.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(CREATE_ROLL_CALL.getAction(), is(Action.CREATE.getAction()));
  }

  @Test
  public void getNameTest() {
    assertThat(CREATE_ROLL_CALL.getName(), is(NAME));
  }

  @Test
  public void getProposedStartTest() {
    assertThat(CREATE_ROLL_CALL.getProposedStart(), is(NOW));
  }

  @Test
  public void getProposedEndTest() {
    assertThat(CREATE_ROLL_CALL.getProposedEnd(), is(END));
  }

  @Test
  public void getDescriptionTest() {
    assertThat(CREATE_ROLL_CALL.getLocation(), is(LOCATION));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(CREATE_ROLL_CALL);
  }

  @Test
  public void equalsTest() {
    assertEquals(CREATE_ROLL_CALL, CREATE_ROLL_CALL);
    assertNotEquals(null, CREATE_ROLL_CALL);

    CreateRollCall createRollCall = new CreateRollCall(ID, NAME, NOW, NOW, END, LOCATION, null);

    assertEquals(CREATE_ROLL_CALL, createRollCall);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(
        java.util.Objects.hash(ID, NAME, NOW, NOW, END, LOCATION, null),
        CREATE_ROLL_CALL.hashCode());
  }
}
