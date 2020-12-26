package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import org.junit.Ignore;
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

public class ElectionTest {

    private final String name1 = "Election name 1";
    private final String name2 = "Election name 2";
    private final Date time = (new Date());
    private final String lao = "0x5932";
    private final ArrayList<String> options = new ArrayList<>(Arrays.asList("0x3434", "0x3333"));
    private final ArrayList<String> optionsWithNull = new ArrayList<>(Arrays.asList("0x3939", null));
    private final Election election1 = new Election(name1, time, lao, options);
    private final Election election2 = new Election(name2, time, lao, options);

    @Test
    public void createElectionNullParametersTest() {
        assertThrows(IllegalArgumentException.class, () -> new Election(null, time, lao, options));
        assertThrows(IllegalArgumentException.class, () -> new Election(name1, null, lao, options));
        assertThrows(IllegalArgumentException.class, () -> new Election(name1, time, null, options));
        assertThrows(IllegalArgumentException.class, () -> new Election(name1, time, lao, null));
        assertThrows(IllegalArgumentException.class, () -> new Election(name1, time, lao, optionsWithNull));
    }

    @Test
    public void getNameTest() {
        assertThat(election1.getName(), is(name1));
    }

    @Test
    public void getTimeTest() {
        assertThat(election1.getTime(), is(time.getTime() / 1000L));
    }

    @Test
    public void getIdTest() {
        assertThat(election1.getId(), is(Hash.hash(name1, time.getTime())));
    }

    @Test
    public void getLaoTest() {
        assertThat(election1.getLao(), is(lao));
    }

    @Test
    public void getOptionsTest() {
        assertThat(election1.getOptions(), is(options));
    }

    @Ignore("Need the private key of the organizer, will test later")
    @Test
    public void getAttestationTest() {
        //TODO: get private key of organizer
        String organizer = new Keys().getPrivateKey();
        ArrayList<String> attestation = new ArrayList<>(Collections.singletonList(
                Signature.sign(organizer, election1.getId())));
        assertThat(election1.getAttestation(), is(attestation));
    }

    @Test
    public void equalsTest() {
        assertEquals(election1, election1);
        assertNotEquals(election1, election2);
    }

    @Test
    public void hashCodeTest() {
        assertEquals(election1.hashCode(), election1.hashCode());
        assertNotEquals(election1.hashCode(), election2.hashCode());
    }
}
