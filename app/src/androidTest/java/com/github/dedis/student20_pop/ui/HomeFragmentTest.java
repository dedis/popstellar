package com.github.dedis.student20_pop.ui;

import android.widget.Adapter;

import androidx.test.core.app.ActivityScenario;

import com.github.dedis.student20_pop.AttendeeActivity;
import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

//TODO: Update tests when the data between activities will be passed
public class HomeFragmentTest {

    @Before
    public void launchActivity() {
        ActivityScenario.launch(MainActivity.class);
    }

    @Test
    public void homeFragmentIsDisplayed(){
        onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
    }

    @Test
    public void listOfLaosIsDisplayed(){
        onView(withId(R.id.lao_list)).check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(Lao.class)))).atPosition(0).check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(Lao.class)))).atPosition(1).check(matches(isDisplayed()));
        onData(allOf(is(instanceOf(Lao.class)))).atPosition(2).check(matches(isDisplayed()));
    }

    @Test
    public void  clickOnLaoWhichOfIAmOrganizerStartsOrganizer(){
        onData(allOf(is(instanceOf(Lao.class)))).atPosition(1).perform(click());
        onView(withId(R.id.fragment_organizer)).check(matches(isDisplayed()));
    }

    @Test
    public void clickOnLaoWhichOfIAmAttendeeStartsAttendee(){
        onData(allOf(is(instanceOf(Lao.class)))).atPosition(0).perform(click());
        onView(withId(R.id.fragment_attendee)).check(matches(isDisplayed()));
    }
}
