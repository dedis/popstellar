package com.github.dedis.student20_pop;

import com.github.dedis.student20_pop.model.Event;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class EventsTest {

    private final String name1 = "Event name 1";
    private final String name2 = "Event name 2";
    private final Date time = (new Date());
    private final String lao = "0x5932";
    private final String location = "EPFL";
    private final String type = "Roll-Call";
    private final ArrayList<String> attendees = new ArrayList<>(Arrays.asList("0x3434", "0x3333"));
    private final ArrayList<String> attendeesWithNull = new ArrayList<>(Arrays.asList("0x3939", null));
    private final Event event1 = new Event(name1, time, lao, location, type);
    private final Event event2 = new Event(name2, time, lao, location, type);

    @Test
    public void createEventWithNullParametersTest() {
        assertThrows(IllegalArgumentException.class, () -> new Event(null, time, lao, location, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, null, lao, location, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, time, null, location, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, time, lao, null, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, time, lao, location, null));
    }

    @Test
    public void getNameTest() {
        assertThat(event1.getName(), is(name1));
    }

    @Test
    public void getTimeTest() {
        assertThat(event1.getTime(), is(time.getTime() / 1000L));
    }

    @Test
    public void getIdTest() {
        assertThat(event1.getId(), is(name1+time));
    }

    @Test
    public void getLaoTest() {
        assertThat(event1.getLao(), is(lao));
    }

    @Test
    public void setAndGetAttendeesTest() {
        event1.setAttendees(attendees);
        assertThat(event1.getAttendees(), is(attendees));
    }

    @Test
    public void getLocationTest() {
        assertThat(event1.getLocation(), is(location));
    }

    @Test
    public void getTypeTest() {
        assertThat(event1.getType(), is(type));
    }

    @Test
    public void getAttestationTest() {
        assertThat(event1.getAttestation(),
                is(new ArrayList<>(Collections.singletonList(name1 + time + lao + location))));
    }

    @Test
    public void setNullAttendeesTest() {
        assertThrows(IllegalArgumentException.class, () -> event1.setAttendees(null));
        assertThrows(IllegalArgumentException.class, () -> event1.setAttendees(attendeesWithNull));
    }

    @Test
    public void equalsTest() {
        assertEquals(event1, event1);
        assertNotEquals(event1, event2);
    }

    @Test
    public void hashCodeTest() {
        assertEquals(event1.hashCode(), event1.hashCode());
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }
}
