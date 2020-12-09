package com.github.dedis.student20_pop.ui;

import androidx.test.core.app.ActivityScenario;

import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;

public class LaunchFragmentTest {

    @Before
    public void launchActivity() {
        ActivityScenario.launch(MainActivity.class);
    }

    @Test
    public void launchNewLaoSetsInfoTest() {
        onView(withId(R.id.tab_launch)).perform(click());
        onView(withId(R.id.entry_box_launch)).perform(typeText("LAO"), closeSoftKeyboard());
        onView(withId(R.id.button_launch)).perform(click());

        ActivityScenario.launch(OrganizerActivity .class).onActivity(a -> {
            PoPApplication app = (PoPApplication) a.getApplication();
            assertThat(app.getLaos().get(0).getName(), is("LAO"));
            assertThat(app.getPerson().getName(), is(PoPApplication.USERNAME));
            assertThat(app.getPerson().getLaos().get(0), is(app.getLaos().get(0).getId()));
        });
    }
}
