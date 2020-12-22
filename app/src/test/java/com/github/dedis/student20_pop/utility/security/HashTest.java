package com.github.dedis.student20_pop.utility.security;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class HashTest {

    @Test
    public void hashNullDataTest() {
        assertThrows(IllegalArgumentException.class, () -> Hash.hash((String) null));
    }

    @Test
    public void hashTest() {
        assertNotNull(Hash.hash("Data to hash"));
    }

    // Hashing : test 0 \fwa"fwa-fwa
    // Expected : ["test","0","\\fwa\"fwa-fwa"]

    @Test
    public void objectsHashingWorks() {
        assertEquals(Hash.hash("[\"test\",\"0\",\"\\\\fwa\\\"fwa-fwa\"]"), Hash.hash("test", 0, "\\fwa\"fwa-fwa"));
    }
}
