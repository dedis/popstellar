package com.github.dedis.student20_pop.utility.security;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.R;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PrivateInfoStorageTest {

    @Before
    public void launchActivity() {
        ActivityScenario.launch(MainActivity.class);
    }

    @Test
    public void storeDataTest() {
        ActivityScenario.launch(MainActivity.class).onActivity(activity -> {
            PrivateInfoStorage.storeData(activity, "TEST","DATA");
            assertThat(PrivateInfoStorage.readData(activity, "TEST"), is("DATA"));
        });
    }
}
