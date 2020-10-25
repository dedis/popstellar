package com.github.dedis.student20_pop;

import com.github.dedis.student20_pop.model.Person;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PersonTest {

    private final String name1 = "Person name 1";
    private final String name2 = "Person name 2";
    private final ArrayList<String> laos = new ArrayList<>(Arrays.asList("0x3939", "0x4747"));
    private final ArrayList<String> laos_with_null = new ArrayList<>(Arrays.asList("0x3939", null, "0x4747"));
    private final Person person1 = new Person(name1);
    private final Person person2 = new Person(name2);

    @Test
    public void getNameTest() {
        assertThat(person1.getName(), is(name1));
    }

    @Test
    public void getIdTest() {
        assertThat(person1.getId(), is(""));
    }

    @Test
    public void getAuthenticationTest() {
        assertThat(person1.getAuthentication(), is(""));
    }

    @Test
    public void setAndGetLaosTest() {
        person1.setLaos(laos);
        assertThat(person1.getLaos(), is(laos));
    }

    @Test (expected = IllegalArgumentException.class)
    public void setNullLaosTest() {
        person1.setLaos(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void setLaosWithNullValueTest() {
        person1.setLaos(laos_with_null);
    }

    @Test
    public void equalsTest() {
        assertEquals(person1, person1);
        assertNotEquals(person2, person1);
    }

    @Test
    public void hashCodeTest() {
        assertEquals(person1.hashCode(), person1.hashCode());
        assertNotEquals(person1.hashCode(), person2.hashCode());
    }
}
