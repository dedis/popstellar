package com.github.dedis.student20_pop.model.event;

import androidx.databinding.ObservableArrayList;

import com.github.dedis.student20_pop.model.Keys;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class RollCallEventTest {

    private final String name1 = "Meeting 1";
    private final String name2 = "Meeting 2";
    private final Date startDate = (new Date());
    private final Date endDate = (new Date());
    private final Date startTime = (new Date());
    private final Date endTime = (new Date());
    private final String lao = new Keys().getPublicKey();
    private final ObservableArrayList<String> attendees = new ObservableArrayList<>();
    private final String location = "EPFL";
    private final String description = "Important information";
    private final RollCallEvent event1 = new RollCallEvent(name1, startDate, endDate, startTime, endTime, lao, attendees, location, description);
    private final RollCallEvent event2 = new RollCallEvent(name2, startDate, endDate, startTime, endTime, lao, attendees, location, description);

    @Test
    public void createEventWithNullParametersTest() {
        assertThrows(IllegalArgumentException.class, () -> new RollCallEvent(name2, null, endDate, startTime, endTime, lao, attendees, location, description));
        assertThrows(IllegalArgumentException.class, () -> new RollCallEvent(name2, startDate, null, startTime, endTime, lao, attendees, location, description));
        assertThrows(IllegalArgumentException.class, () -> new RollCallEvent(name2, startDate, endDate, null, endTime, lao, attendees, location, description));
        assertThrows(IllegalArgumentException.class, () -> new RollCallEvent(name2, startDate, endDate, startTime, null, lao, attendees, location, description));
        assertThrows(IllegalArgumentException.class, () -> new RollCallEvent(name2, startDate, endDate, startTime, endTime, lao, null, location, description));
        assertThrows(IllegalArgumentException.class, () -> new RollCallEvent(name2, startDate, endDate, startTime, endTime, lao, attendees, location, null));
    }

    @Test
    public void getStartDateTest() {
        assertThat(event1.getStartDate(), is(startDate));
    }

    @Test
    public void getEndDateTest() {
        assertThat(event1.getEndDate(), is(endDate));
    }

    @Test
    public void getStartTimeTest() {
        assertThat(event1.getStartTime(), is(startTime));
    }

    @Test
    public void getEndTimeTest() {
        assertThat(event1.getEndTime(), is(endTime));
    }

    @Test
    public void getDescriptionTest() {
        assertThat(event1.getDescription(), is(description));
    }

    @Test
    public void addAttendeeTest() {
        String attendee = new Keys().getPublicKey();
        event1.addAttendee(attendee);
        attendees.add(attendee);
        assertThat(event1.getAttendees(), is(attendees));
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
