package com.github.dedis.student20_pop;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.event.Event;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.student20_pop.model.event.EventType.DISCUSSION;
import static com.github.dedis.student20_pop.model.event.EventType.MEETING;
import static com.github.dedis.student20_pop.model.event.EventType.POLL;

public class AttendeeActivityTest {

    @Rule
    public ActivityScenarioRule<AttendeeActivity> activityScenarioRule =
            new ActivityScenarioRule<>(AttendeeActivity.class);

    @Before
    public void launchActivity() {
        activityScenarioRule.getScenario().onActivity(
                activity -> {
                    PoPApplication app = (PoPApplication) activity.getApplication();

                    String notMyPublicKey = new Keys().getPublicKey();
                    Lao lao1 = new Lao("LAO 1", notMyPublicKey);
                    Lao lao2 = new Lao("LAO 2", notMyPublicKey);
                    Lao lao3 = new Lao("My LAO 3", app.getPerson().getId());

                    List<Event> events2 = Arrays.asList(
                            new Event("Future Event 1", lao2.getId(), 2617547969L, "EPFL", POLL),
                            new Event("Present Event 1", lao2.getId(), Instant.now().getEpochSecond(), "Somewhere", DISCUSSION),
                            new Event("Past Event 1", lao2.getId(), 1481643086L, "Here", MEETING));

                    lao2.setEvents(events2);

                    app.createLao(lao1);
                    app.createLao(lao2);
                    app.createLao(lao3);
                    app.setCurrentLao(lao2);
                }
        );
    }

    @Test
    public void onClickHomeTest() {
        onView(withId(R.id.tab_home)).perform(click());
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickPropertiesTest() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickListEventsTest() {
        onView(withText("Past Events")).perform(click());
        onView(withText("Present Events")).perform(click());
        onView(withText("Future Events")).perform(click());
        onView(withId(R.id.event_layout)).check(matches(isDisplayed()));
    }
}
