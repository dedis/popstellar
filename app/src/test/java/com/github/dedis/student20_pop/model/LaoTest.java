package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.model.event.MeetingEvent;
import com.github.dedis.student20_pop.model.event.PollEvent;

import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class LaoTest {

  private static final String LAO_NAME_1 = "LAO name 1";
  private static final String LAO_NAME_2 = "LAO name 2";

  private static final String ORGANIZER = new Keys().getPublicKey();

  private static final String WITNESS = "0x3435";
  private static final List<String> WITNESSES = Arrays.asList("0x3434", "0x4747");
  private static final List<String> WITNESSES_WITH_NULL = Arrays.asList("0x3939", null, "0x4747");

  private static final URI HOST = URI.create("ws://localhost:8000");

  private static final Lao LAO_1 = new Lao(LAO_NAME_1, ORGANIZER, HOST);
  private static final Lao LAO_FOR_EVENTS = new Lao(LAO_NAME_2, ORGANIZER, HOST);

  private static final List<Event> EVENTS =
          Arrays.asList(
                  new MeetingEvent("meeting", 0, 0, LAO_FOR_EVENTS.getId(), "loc", "desc"),
                  new PollEvent("question", 0, 0, LAO_FOR_EVENTS.getId(), "loc", new ArrayList<>(), false));
  private static final List<Event> EVENTS_WITH_NULL =
          Arrays.asList(
                  new MeetingEvent("meeting", 0, 0, LAO_FOR_EVENTS.getId(), "loc", "desc"),
                  null,
                  new PollEvent("question", 0, 0, LAO_FOR_EVENTS.getId(), "loc", new ArrayList<>(), false));

  private static final List<Lao> LAOS = new ArrayList<>(Arrays.asList(LAO_1, LAO_FOR_EVENTS));
  private static final List<Lao> LAOS_WITH_NULL =
      new ArrayList<>(Arrays.asList(LAO_1, null, LAO_FOR_EVENTS));

  @Test
  public void createLaoNullParametersTest() {
    assertThrows(IllegalArgumentException.class, () -> new Lao(null, ORGANIZER, HOST));
    assertThrows(IllegalArgumentException.class, () -> new Lao(LAO_NAME_1, null, HOST));
    assertThrows(IllegalArgumentException.class, () -> new Lao(LAO_NAME_1, ORGANIZER, null));
  }

  @Test
  public void createLaoEmptyNameTest() {
    assertThrows(IllegalArgumentException.class, () -> new Lao("", ORGANIZER, HOST));
    assertThrows(IllegalArgumentException.class, () -> new Lao("     ", ORGANIZER, HOST));
  }

  @Test
  public void setAndGetNameTest() {
    Lao lao = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    assertThat(lao.getName(), is(LAO_NAME_1));
    lao.setName(LAO_NAME_2);
    assertThat(lao.getName(), is(LAO_NAME_2));
  }

  @Test
  public void getTimeTest() {
    final int LENGTH_UNIX_TIMESTAMP = 10;
    assertThat(Long.toString(LAO_1.getTime()).length(), is(LENGTH_UNIX_TIMESTAMP));
  }

  @Test
  public void getOrganizerTest() {
    assertThat(LAO_1.getOrganizer(), is(ORGANIZER));
  }

  @Test
  public void getHostTest() {
    assertThat(LAO_1.getHost(), is(HOST));
  }

  @Test
  public void setAndGetWitnessesTest() {
    Lao lao = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    lao.setWitnesses(WITNESSES);
    assertThat(lao.getWitnesses(), is(WITNESSES));
  }

  @Test
  public void addWitnessTest() {
    Lao lao = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    lao.addWitness(WITNESS);
    assertThat(lao.getWitnesses(), hasItem(WITNESS));
  }

  @Test
  public void setAndGetMembersTest() {
    Lao lao = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    lao.setMembers(WITNESSES);
    assertThat(lao.getMembers(), is(WITNESSES));
  }

  @Test
  public void setAndGetEventsTest() {
    LAO_FOR_EVENTS.setEvents(EVENTS);
    assertThat(LAO_FOR_EVENTS.getEvents(), is(EVENTS));
  }

  @Test
  public void addEventTest() {
    Lao lao = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    Event event = new PollEvent("question", 0, 0, lao.getId(), "loc", new ArrayList<>(), false);
    lao.addEvent(event);
    assertThat(lao.getEvents(), hasItem(event));
  }

  @Test
  public void setNullNameTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setName(null));
  }

  @Test
  public void setEmptyNameTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setName(""));
  }

  @Test
  public void setNullWitnessesTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setWitnesses(null));
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setWitnesses(WITNESSES_WITH_NULL));
  }

  @Test
  public void setNullMembersTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setMembers(null));
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setMembers(WITNESSES_WITH_NULL));
  }

  @Test
  public void setNullEventsTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.addEvent(null));
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setEvents(null));
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setEvents(EVENTS_WITH_NULL));
  }

  @Test
  public void getNullIdsTest() {
    assertThrows(IllegalArgumentException.class, () -> Lao.getIds(null));
    assertThrows(IllegalArgumentException.class, () -> Lao.getIds(LAOS_WITH_NULL));
  }

  @Test
  public void getIdsTest() {
    assertThat(
        Lao.getIds(LAOS),
        is(new ArrayList<>(Arrays.asList(LAO_1.getId(), LAO_FOR_EVENTS.getId()))));
  }

  @Test
  public void equalsTest() {
    Lao lao1 = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    Lao lao2 = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    Lao lao3 = new Lao(LAO_NAME_2, ORGANIZER, HOST);

    assertEquals(lao1, lao2);
    assertNotEquals(lao1, lao3);
  }

  @Test
  public void hashCodeTest() {
    Lao lao1 = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    Lao lao2 = new Lao(LAO_NAME_1, ORGANIZER, HOST);
    Lao lao3 = new Lao(LAO_NAME_2, ORGANIZER, HOST);

    assertEquals(lao1.hashCode(), lao2.hashCode());
    assertNotEquals(lao1.hashCode(), lao3.hashCode());
  }
}
