package com.github.dedis.student20_pop.utility.security;

import androidx.test.core.app.ActivityScenario;

import com.github.dedis.student20_pop.MainActivity;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PrivateInfoStorageTest {

    @Test
    public void storeAndReadDataTest() {
        ActivityScenario.launch(MainActivity.class).onActivity(activity -> {
            assertTrue(PrivateInfoStorage.storeData(activity, "TEST", "DATA"));
            assertThat(PrivateInfoStorage.readData(activity, "TEST"), is("DATA"));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContextStoreTest() {
        PrivateInfoStorage.storeData(null, "TEST", "DATA");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContextReadTest() {
        PrivateInfoStorage.readData(null, "TEST");
    }
}
