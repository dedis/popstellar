package com.github.dedis.student20_pop.ui;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;

public class PollEventCreationFragmentTest {

    private final String question = "Question";
    private final String choice1 = "blabla";
    private final String choice2 = "second choice";

    @Rule
    public ActivityScenarioRule<OrganizerActivity> activityScenarioRule =
            new ActivityScenarioRule<>(OrganizerActivity.class);
    private View decorView;

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

        onView(allOf(withId(R.id.add_future_event_button), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
        onView(withText(getApplicationContext().getString(R.string.poll_event))).perform(click());
    }

    @Test
    public void canLaunchOrganizerPollFragment() {
        onView(withId(R.id.fragment_organizer_poll)).check(matches(isDisplayed()));
    }

    @Test
    public void chooseOneOfNIsDefault() {
        onView(withId(R.id.radio_poll_type_1)).check(matches(isChecked()));
        onView(withId(R.id.radio_poll_type_2)).check(matches(isNotChecked()));
    }

    @Test
    public void oneChoiceByDefault() {
        onData(is(instanceOf(String.class))).onChildView(withText("1")).check(matches(isDisplayed()));
        onData(is(instanceOf(String.class))).onChildView(withText("2")).check(doesNotExist());
    }

    @Test
    public void scheduleButtonIsDisabled() {
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void cancelButtonWorks() {
        onView(withId(R.id.cancel_button)).perform(click());
    }

    @Test
    public void plusButtonAddsOneChoice() {
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).check(matches(isDisplayed()));
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(2).check(matches(isDisplayed()));
    }

    @Test
    public void deleteChoiceDeletesTheCorrectChoice() {
        onView(withId(R.id.button_add)).perform(click());
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(typeText("blabla"));
        closeSoftKeyboard();
        onData(allOf(is(instanceOf(String.class)), is(choice1))).onChildView(withId(R.id.delete_choice_button)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).check(matches(not(is(choice1))));
    }

    @Test
    public void scheduleButtonIsEnabledWhenCorrectFieldsAreFilled() {
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.question_edit_text)).perform(typeText(question));
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
        onData(is(instanceOf(String.class))).atPosition(0).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice1));
        closeSoftKeyboard();
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.button_add)).perform(click());
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice2));
        closeSoftKeyboard();
        onView(withId(R.id.schedule_button)).check(matches(isEnabled()));
    }

    @Test
    @Ignore
    public void scheduleButtonIsDisabledWhenSomeFieldsAreDeleted() {
        onView(withId(R.id.question_edit_text)).perform(typeText(question));
        onData(is(instanceOf(String.class))).atPosition(0).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice1));
        closeSoftKeyboard();
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice2));
        closeSoftKeyboard();
        onView(withId(R.id.schedule_button)).check(matches(isEnabled()));
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.delete_choice_button)).perform(click());
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void scheduleButtonIsDisabledWhenSomeTextFieldsAreDeleted() {
        onView(withId(R.id.question_edit_text)).perform(typeText(question));
        onData(is(instanceOf(String.class))).atPosition(0).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice1));
        closeSoftKeyboard();
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice2));
        closeSoftKeyboard();
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(replaceText(""));
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void scheduleButtonIsDisabledWhenQuestionFieldIsReplacedWithWhiteSpaces() {
        onView(withId(R.id.question_edit_text)).perform(typeText(question));
        onData(is(instanceOf(String.class))).atPosition(0).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice1));
        closeSoftKeyboard();
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice2));
        closeSoftKeyboard();
        onView(withId(R.id.question_edit_text)).perform(replaceText("    "));
        onView(withId(R.id.schedule_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void confirmAddsEventToEventList() {
        onView(withId(R.id.question_edit_text)).perform(typeText(question));

        onData(is(instanceOf(String.class))).atPosition(0).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice1));
        closeSoftKeyboard();
        onView(withId(R.id.button_add)).perform(click());
        onData(is(instanceOf(String.class))).atPosition(1).onChildView(withId(R.id.choice_edit_text)).perform(typeText(choice2));
        closeSoftKeyboard();

        onView(withId(R.id.schedule_button)).check(matches(isEnabled()));
        onView(withId(R.id.schedule_button)).perform(click());

        activityScenarioRule.getScenario().onActivity(
                activity -> {
                    PoPApplication app = (PoPApplication) activity.getApplication();
                    List<Event> events = app.getEvents(app.getCurrentLao());
                    List<String> eventsName = events.stream().map(Event::getName).collect(Collectors.toList());
                    Assert.assertThat(question, isIn(eventsName));
                }
        );
    }
}
