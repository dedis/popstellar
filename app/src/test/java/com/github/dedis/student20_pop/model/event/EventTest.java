package com.github.dedis.student20_pop.model.event;

import androidx.databinding.ObservableArrayList;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class EventTest {

    private final String name1 = new Keys().getPublicKey();
    private final String name2 = new Keys().getPublicKey();
    private final long startTime = Instant.now().getEpochSecond();
    private final String lao = new Keys().getPublicKey();
    private final String location = "EPFL";
    private final EventType type = EventType.ROLL_CALL;
    private final ObservableArrayList<String> attendees = new ObservableArrayList<>();
    private final ObservableArrayList<String> attendeesWithNull = new ObservableArrayList<>();
    private final Event event1 = new Event(name1, lao, startTime, location, type);
    private final Event event2 = new Event(name2, lao, startTime, location, type);

    @Test
    public void createEventWithNullParametersTest() {
        assertThrows(IllegalArgumentException.class, () -> new Event(null, lao, startTime, location, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, null, startTime, location, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, lao, startTime, null, type));
        assertThrows(IllegalArgumentException.class, () -> new Event(name1, lao, startTime, location, null));
    }

    @Test
    public void getNameTest() {
        assertThat(event1.getName(), is(name1));
        assertThat(event2.getName(), is(name2));
    }

    @Test
    public void getStartTimeTest() {
    assertThat(event1.getStartTime(), is(startTime));
    }

    @Test
    public void getIdTest() {
        assertThat(event1.getId(), is(Hash.hash(type.getSuffix(), lao, event1.getTime(), name1)));
        assertThat(event2.getId(), is(Hash.hash(type.getSuffix(), lao, event2.getTime(), name2)));
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
    public void setNullAttendeesTest() {
        attendees.addAll(Arrays.asList("0x3434", "0x3333"));
        attendeesWithNull.addAll(Arrays.asList("0x3323", null));
        assertThrows(IllegalArgumentException.class, () -> event1.setAttendees(null));
        assertThrows(IllegalArgumentException.class, () -> event1.setAttendees(attendeesWithNull));
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
