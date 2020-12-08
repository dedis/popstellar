package com.github.dedis.student20_pop.ui;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;

import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;


public class OrganizerFragmentTest {

    @Before
    public void launchActivity() {
        ActivityScenario.launch(OrganizerActivity.class);
    }

    @Test
    public void onClickPropertiesTest() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
    }

    @Test
    public void onClickListEventsTest() {
        onView(withText(getApplicationContext().getString(R.string.past_events))).perform(click());
        onView(withText(getApplicationContext().getString(R.string.present_events))).perform(click());
        onView(withText(getApplicationContext().getString(R.string.future_events))).perform(click());
        onView(withId(R.id.event_layout)).check(matches(isDisplayed()));
    }

    @Test
    public void clickOnAddEventButtonOpensDialog() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
    }

    @Test
    public void canCancelAddEvent() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
        onView(withId(android.R.id.button2)).perform(click());
    }

    @Test
    public void canShowEditProperties() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onView(withId(R.id.properties_edit_view)).check(matches(isDisplayed()));
    }

    @Test
    public void canHideProperties() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.viewSwitcher)).check(matches(not(isDisplayed())));
    }

    @Test
    public void canEditLaoTitleAndConfirm() {
        String stringToBeTyped = "string to be typed";
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onView(withId(R.id.organization_name_editText)).perform(clearText());
        onView(withId(R.id.organization_name_editText)).perform(typeText(stringToBeTyped));
        onView(withId(R.id.properties_edit_confirm)).perform(click());
        onView(allOf(withText(stringToBeTyped), withId(R.id.organization_name))).check(matches(isDisplayed()));
    }

    @Test
    public void canCancelEditProperties() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onView(withId(R.id.organization_name_editText)).perform(clearText());
        onView(withId(R.id.properties_edit_cancel)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
    }

    @Test
    public void clickOnDeleteOpensDialog() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onData(anything()).inAdapterView(withId(R.id.witness_edit_list))
                .atPosition(0).onChildView(withId(R.id.image_button_delete_witness)).check(matches(isDisplayed())).perform(click());
        onView(withText("Delete ?")).check(matches(isDisplayed()));
    }

    @Test
    public void canCancelDeleteWitness() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onData(anything()).inAdapterView(withId(R.id.witness_edit_list))
                .atPosition(0).onChildView(withId(R.id.image_button_delete_witness)).check(matches(isDisplayed())).perform(click());
        onView(withText("Delete ?")).check(matches(isDisplayed()));
        onView(withText(R.string.button_cancel)).perform(click());
        onData(anything()).inAdapterView(withId(R.id.witness_edit_list))
                .atPosition(0).onChildView(withId(R.id.image_button_delete_witness)).check(matches(isDisplayed()));
    }

    @Test
    public void canDeleteWitness() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onData(anything()).inAdapterView(withId(R.id.witness_edit_list))
                .atPosition(0).onChildView(withId(R.id.image_button_delete_witness)).check(matches(isDisplayed())).perform(click());
        onView(withText("Delete ?")).check(matches(isDisplayed()));
        onView(withText(R.string.button_confirm)).perform(click());
    }
    
    @Test
    @Ignore("TODO : Check that scanning a Witness QR code adds witness to witness list")
    public void canAddWitness() {
    }

    @Test
    @Ignore("TODO : Check that the corresponding Fragment has been launched")
    public void canLaunchCreateMeetingEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).perform(click());
    }

    @Test
    @Ignore("TODO : Check that the corresponding Fragment has been launched")
    public void canLaunchCreateRollCallEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).perform(click());
    }

    @Test
    @Ignore("TODO : Check that the corresponding Fragment has been launched")
    public void canLaunchCreatePollEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.poll_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).perform(click());
    }
}
