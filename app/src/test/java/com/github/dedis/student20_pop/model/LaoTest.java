package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

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

public class LaoTest {

    private final String lao1_name = "LAO name 1";
    private final String lao2_name = "LAO name 2";
    private final Date time = (new Date());
    private final String organizer = new Keys().getPublicKey();
    private final ArrayList<String> list = new ArrayList<>(Arrays.asList("0x3434", "0x4747"));
    private final ArrayList<String> listWithNull = new ArrayList<>(Arrays.asList("0x3939", null, "0x4747"));
    private final Lao lao1 = new Lao(lao1_name, time, organizer);
    private final Lao lao2 = new Lao(lao2_name, time, organizer);
    private final List<Lao> laos = new ArrayList<>(Arrays.asList(lao1, lao2));
    private final List<Lao> laosWithNull = new ArrayList<>(Arrays.asList(lao1, null, lao2));

    @Test
    public void createLaoNullParametersTest() {
        assertThrows(IllegalArgumentException.class, () -> new Lao(null, time, organizer));
        assertThrows(IllegalArgumentException.class, () -> new Lao(lao1_name, null, organizer));
        assertThrows(IllegalArgumentException.class, () -> new Lao(lao1_name, time, null));
    }

    @Test
    public void createLaoEmptyNameTest() {
        assertThrows(IllegalArgumentException.class, () -> new Lao("", time, organizer));
        assertThrows(IllegalArgumentException.class, () -> new Lao("     ", time, organizer));
    }

    @Test
    public void setAndGetNameTest() {
        assertThat(lao1.getName(), is(lao1_name));
        assertThat((lao1.setName(lao2_name)).getName(), is(lao2_name));
    }

    @Test
    public void getTimeTest() {
        assertThat(lao1.getTime(), is(time.getTime() / 1000L));
    }

    @Test
    public void getIdTest() {
        assertThat(lao1.getId(), is(Hash.hash(lao1_name, time.getTime())));
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
        assertThat(lao1.getAttestation(), is(Signature.sign(organizer, lao1_name + time + organizer)));
    }

    @Test
    public void setNullNameTest() {
        assertThrows(IllegalArgumentException.class, () -> lao1.setName(null));
    }

    @Test
    public void setEmptyNameTest() {
        assertThrows(IllegalArgumentException.class, () -> lao1.setName(""));
    }

    @Test
    public void setNullWitnessesTest() {
        assertThrows(IllegalArgumentException.class, () -> lao1.setWitnesses(null));
        assertThrows(IllegalArgumentException.class, () -> lao1.setWitnesses(listWithNull));
    }

    @Test
    public void setNullMembersTest() {
        assertThrows(IllegalArgumentException.class, () -> lao1.setMembers(null));
        assertThrows(IllegalArgumentException.class, () -> lao1.setMembers(listWithNull));
    }

    @Test
    public void setNullEventsTest() {
        assertThrows(IllegalArgumentException.class, () -> lao1.setEvents(null));
        assertThrows(IllegalArgumentException.class, () -> lao1.setEvents(listWithNull));
    }

    @Test
    public void getNullIdsTest() {
        assertThrows(IllegalArgumentException.class, () -> Lao.getIds(null));
        assertThrows(IllegalArgumentException.class, () -> Lao.getIds(laosWithNull));
    }

    @Test
    public void getIdsTest() {
        assertThat(Lao.getIds(laos), is(new ArrayList<>(Arrays.asList(lao1.getId(), lao2.getId()))));
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
