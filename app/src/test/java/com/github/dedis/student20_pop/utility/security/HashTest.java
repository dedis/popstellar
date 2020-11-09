package com.github.dedis.student20_pop.utility.security;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class HashTest {

    @Test
    public void hashNullDataTest() {
        assertThrows(IllegalArgumentException.class, () -> Hash.hash(null));
    }

    @Test
    public void hashTest() {
        assertNotNull(Hash.hash("Data to hash"));
    }
}
