/*package com.github.dedis.student20_pop.detail.fragments.event.creation;

import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

public class ElectionSetupFragmentTest {

    @Rule
    public ActivityScenarioRule<LaoDetailActivity> activityScenarioRule = new ActivityScenarioRule<>(LaoDetailActivity.class);


    @Before
    public void setUp() {
         activityScenarioRule.getScenario().onActivity(activity -> {

         });
         onView(allOf(withId(R.id.add_future_event_button), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
         onView(withText("Election Event")).perform(click());
    }

    @Test
    public void fillUpAllRequiredFieldsEnablesSubmitButton() {

        onView(withId(R.id.election_submit_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.election_setup_name)).perform(replaceText("my election"));

        assert(sleep(2000));

        onView(withId(R.id.start_date_edit_text)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(
                PickerActions.setDate(
                        2025, 0, 1
                )
        );

        assert(sleep(2000));

        onView(withId(R.id.start_time_edit_text)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(
                PickerActions.setDate(
                        2025, 0, 20
                )
        );

        assert(sleep(2000));


        onView(withId(R.id.start_time_edit_text)).perform(click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(
                PickerActions.setTime(
                        9, 15
                )
        );

        assert(sleep(2000));

        onView(withId(R.id.end_time_edit_text)).perform(click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(
                PickerActions.setTime(
                        9, 15
                )
        );

        assert(sleep(2000));

        onView(withId(R.id.election_question)).perform(replaceText("my question"));

        assert(sleep(2000));

        onView(withId(R.id.new_ballot_option_text)).perform(replaceText("ballot option 1"));

        assert(sleep(2000));

        onView(allOf(withId(R.id.new_ballot_option_text), withText(""))).perform(replaceText("ballot option 2"));

        assert(sleep(2000));

        onView(withId(R.id.election_submit_button)).check(matches(isEnabled()));
    }

    @Test
    public void laoTextIsCorrect() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(activity);
            onView(withId(R.id.election_setup_lao_name)).check(matches(withText(laoDetailViewModel.getCurrentLao().getName())));
        });

    }

    private boolean sleep(int millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
} */
