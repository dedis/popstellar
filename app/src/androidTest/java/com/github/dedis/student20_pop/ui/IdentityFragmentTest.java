package com.github.dedis.student20_pop.ui;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

public class IdentityFragmentTest {
    private View decorView;

    @Rule
    public ActivityScenarioRule<OrganizerActivity> activityScenarioRule =
            new ActivityScenarioRule<>(OrganizerActivity.class);

    @Before
    public void setUp() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<OrganizerActivity>() {

            /**
             * This method is invoked on the main thread with the reference to the Activity.
             *
             * @param activity an Activity instrumented by the {@link ActivityScenario}. It never be null.
             */
            @Override
            public void perform(OrganizerActivity activity) {
                decorView = activity.getWindow().getDecorView();
            }
        });

        onView(withId(R.id.tab_identity)).perform(click());
    }

    @Test
    public void canOpenIdentityFragment() {
        onView(withId(R.id.checkbox_anonymous)).check(matches(isDisplayed()));
    }

    @Test
    public void userIsAnonymousByDefault() {
        onView(withId(R.id.checkbox_anonymous)).check(matches(isChecked()));
    }

    @Test
    public void basicIdentityInfosAreHiddenWhenAnonymous() {
        onView(withId(R.id.identity_phone)).check(matches(not(isDisplayed())));
        onView(withId(R.id.identity_title)).check(matches(not(isDisplayed())));
        onView(withId(R.id.identity_organization)).check(matches(not(isDisplayed())));
        onView(withId(R.id.identity_name)).check(matches(not(isDisplayed())));
        onView(withId(R.id.identity_email)).check(matches(not(isDisplayed())));
    }

    @Test
    public void basicIdentityInfosAreShownWhenNotAnonymous() {
        onView(withId(R.id.checkbox_anonymous)).perform(click());
        onView(withId(R.id.identity_phone)).check(matches(isDisplayed()));
        onView(withId(R.id.identity_title)).check(matches(isDisplayed()));
        onView(withId(R.id.identity_organization)).check(matches(isDisplayed()));
        onView(withId(R.id.identity_name)).check(matches(isDisplayed()));
        onView(withId(R.id.identity_email)).check(matches(isDisplayed()));
    }
}
