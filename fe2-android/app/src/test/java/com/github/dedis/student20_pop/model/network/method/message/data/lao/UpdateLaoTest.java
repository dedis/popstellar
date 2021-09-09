package com.github.dedis.student20_pop.model.network.method.message.data.lao;

import com.github.dedis.student20_pop.utility.network.IdGenerator;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class UpdateLaoTest {
    private final String name = "New name";
    private final long lastModified = 972;

    private final Set<String> witnesses = new HashSet<>(Arrays.asList("0x3434", "0x4747"));
    private final UpdateLao updateLao = new UpdateLao("organizer", 10, name, lastModified, witnesses);

    @Test
    public void getNameTest() {
        assertThat(updateLao.getName(), is(name));
    }

    @Test
    public void getLastModifiedTest() {
        assertThat(updateLao.getLastModified(), is(lastModified));
    }

    @Test
    public void getIdTest() {
        assertThat(updateLao.getId(), is(IdGenerator.generateLaoId("organizer", 10, name)));
    }

    @Test
    public void getWitnessesTest() {
        assertThat(updateLao.getWitnesses(), is(witnesses));
    }

    @Test
    public void isEqual() {
        assertEquals(updateLao, new UpdateLao("organizer", 10, name, lastModified, witnesses));
        // different creation time so the id won't be the same
        assertNotEquals(updateLao, new UpdateLao("organizer", 20, name, lastModified, witnesses));
        // different organizer so the id won't be the same
        assertNotEquals(updateLao, new UpdateLao("different", 10, name, lastModified, witnesses));
        // different name
        assertNotEquals(updateLao, new UpdateLao("organizer", 10, "random", lastModified, witnesses));
        // different witnesses
        assertNotEquals(updateLao, new UpdateLao("organizer", 10, name, lastModified, new HashSet<>(Arrays.asList("0x3434"))));
    }
}
