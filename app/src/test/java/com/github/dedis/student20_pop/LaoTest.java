package com.github.dedis.student20_pop;

import com.github.dedis.student20_pop.model.Lao;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LaoTest {

    private final String lao1_name = "LAO name 1";
    private final String lao2_name = "LAO name 2";
    private final Date time = (new Date());
    private final String organizer = "0x5932";
    private final ArrayList<String> list = new ArrayList<>(Arrays.asList("0x3434", "0x4747"));
    private final ArrayList<String> l_with_null = new ArrayList<>(Arrays.asList("0x3939", null, "0x4747"));
    private final Lao lao1 = new Lao(lao1_name, time, organizer);
    private final Lao lao2 = new Lao(lao2_name, time, organizer);

    @Test
    public void setAndGetNameTest() {
        assertThat(lao1.getName(), is(lao1_name));
        lao1.setName(lao2_name);
        assertThat(lao1.getName(), is(lao2_name));
    }

    @Test
    public void getTimeTest() {
        assertThat(lao1.getTime(), is(time.getTime() / 1000L));
    }

    @Test
    public void getIdTest() {
        assertThat(lao1.getId(), is(lao1_name+time));
    }

    @Test
    public void getOrganizerTest() {
        assertThat(lao1.getOrganizer(), is(organizer));
    }

    @Test
    public void setAndGetWitnessesTest() {
        lao1.setWitnesses(list);
        assertThat(lao1.getWitnesses(), is(list));
    }

    @Test
    public void setAndGetMembersTest() {
        lao1.setMembers(list);
        assertThat(lao1.getMembers(), is(list));
    }

    @Test
    public void setAndGetEventsTest() {
        lao1.setEvents(list);
        assertThat(lao1.getEvents(), is(list));
    }

    @Test
    public void getAttestationTest() {
        assertThat(lao1.getAttestation(), is(lao1_name + time + organizer));
    }

    @Test (expected = IllegalArgumentException.class)
    public void setNullNameTest() {
        lao1.setName(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setNullWitnessesTest() {
        lao1.setWitnesses(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setNullMembersTest() {
        lao1.setMembers(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setNullEventsTest() {
        lao1.setEvents(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setWitnessesWithNullValueTest() {
        lao1.setWitnesses(l_with_null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setMembersWithNullValueTest() {
        lao1.setMembers(l_with_null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setEventsWithNullValueTest() {
        lao1.setEvents(l_with_null);
    }

    @Test
    public void equalsTest() {
        assertEquals(lao1, lao1);
        assertNotEquals(lao1, lao2);
    }

    @Test
    public void hashCodeTest() {
        assertEquals(lao1.hashCode(), lao1.hashCode());
        assertNotEquals(lao1.hashCode(), lao2.hashCode());
    }
}
