package com.github.dedis.student20_pop.model.event;

import com.github.dedis.student20_pop.model.Keys;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class PollEventTest {

    private final String name1 = "Poll 1";
    private final String name2 = "Poll 2";
    private final Date startDate = (new Date());
    private final Date endDate = (new Date());
    private final Date startTime = (new Date());
    private final Date endTime = (new Date());
    private final String lao = new Keys().getPublicKey();
    private final String location = "EPFL";
    private final List<String> choices = new ArrayList<>(Arrays.asList("Yes", "No"));
    private final List<String> choicesNull = new ArrayList<>(Arrays.asList("Yes", null));
    private final PollEvent event1 = new PollEvent(name1, startDate, endDate, startTime, endTime, lao, location, choices, true);
    private final PollEvent event2 = new PollEvent(name2, startDate, endDate, startTime, endTime, lao, location, choices, false);

    @Test
    public void createEventWithNullParametersTest() {
        assertThrows(IllegalArgumentException.class, () -> new PollEvent(name1, null, endDate, startTime, endTime, lao, location, choices, true));
        assertThrows(IllegalArgumentException.class, () -> new PollEvent(name1, startDate, null, startTime, endTime, lao, location, choices, true));
        assertThrows(IllegalArgumentException.class, () -> new PollEvent(name1, startDate, endDate, null, endTime, lao, location, choices, true));
        assertThrows(IllegalArgumentException.class, () -> new PollEvent(name1, startDate, endDate, startTime, null, lao, location, choices, true));
        assertThrows(IllegalArgumentException.class, () -> new PollEvent(name1, startDate, endDate, startTime, endTime, lao, location, null, true));
        assertThrows(IllegalArgumentException.class, () -> new PollEvent(name1, startDate, endDate, startTime, endTime, lao, location, choicesNull, true));
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
    public void getChoicesTest() {
        assertThat(event1.getChoices(), is(choices));
    }

    @Test
    public void isOneOfNTest() {
        assertThat(event1.isOneOfN(), is(true));
        assertThat(event2.isOneOfN(), is(false));
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
