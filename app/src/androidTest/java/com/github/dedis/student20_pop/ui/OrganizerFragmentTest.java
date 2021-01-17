package com.github.dedis.student20_pop.ui;

import android.Manifest;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;

import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult;
import static com.github.dedis.student20_pop.PoPApplication.AddWitnessResult.ADD_WITNESS_SUCCESSFUL;
import static com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType.ADD_WITNESS;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;


public class OrganizerFragmentTest {
    @Rule
    public final GrantPermissionRule rule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    @Rule
    public ActivityScenarioRule<OrganizerActivity> activityScenarioRule =
            new ActivityScenarioRule<>(OrganizerActivity.class);
    private View decorView;
    private final String WITNESS = "Alphonse";

    /**
     * This is a simple matcher to avoid error when multiple views match in hierarchy
     * (such as in a ListView)
     * <p>
     * More infp here :
     * https://stackoverflow.com/questions/29378552/in-espresso-how-to-avoid-ambiguousviewmatcherexception-when-multiple-views-matc/39756832#39756832
     *
     * @param matcher
     * @param index
     * @return
     */
    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }

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
                PoPApplication app = (PoPApplication) activity.getApplication();
                assertThat(app.getWitnesses(), is(empty()));
                AddWitnessResult result = app.addWitness(WITNESS);
                assertThat(app.getWitnesses(), is(Collections.singletonList(WITNESS)));
                assertThat(result, is(ADD_WITNESS_SUCCESSFUL));
            }
        });
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
        onView(withIndex(withId(R.id.event_layout), 0)).check(matches(isDisplayed()));
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
    public void confirmEmptyLaoTitleShowsToast() {
        String expectedWarning = getApplicationContext().getString(R.string.exception_message_empty_lao_name);
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onView(withId(R.id.organization_name_editText)).perform(clearText());
        onView(withId(R.id.properties_edit_confirm)).perform(click());
        onView(withText(expectedWarning))
                .inRoot(withDecorView(not(decorView)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void canAddWitness() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onView(withId(R.id.add_witness_button)).perform(click());

        // Simulate a detected url
        activityScenarioRule.getScenario().onActivity(a -> {
            Fragment fragment = a.getSupportFragmentManager().findFragmentByTag(QRCodeScanningFragment.TAG);
            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof QRCodeScanningFragment);

            PoPApplication app = (PoPApplication) a.getApplication();
            final String LAO_ID = app.getCurrentLaoUnsafe().getId();
            final String WITNESS_ID = "t9Ed+TEwDM0+u0ZLdS4ZB/Vrrnga0Lu2iMkAQtyFRrQ=";
            final String TEST_IDS = WITNESS_ID + LAO_ID;

            ((QRCodeScanningFragment) fragment).onQRCodeDetected(TEST_IDS, ADD_WITNESS, null);

            for (Lao lao : app.getLaos()) {
                if (lao.getId().equals(LAO_ID)) {
                    List<String> witnesses = lao.getWitnesses();
                    Assert.assertThat(WITNESS_ID, isIn(witnesses));
                }
            }
        });

        onView(withText(getApplicationContext().getString(R.string.add_witness_successful)))
                .inRoot(withDecorView(not(decorView)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void addingTwiceSameWitnessShowsToast() {
        onView(withId(R.id.tab_properties)).perform(click());
        onView(withId(R.id.properties_view)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_button)).perform(click());
        onView(withId(R.id.add_witness_button)).perform(click());

        // Simulate a detected url
        activityScenarioRule.getScenario().onActivity(a -> {
            Fragment fragment = a.getSupportFragmentManager().findFragmentByTag(QRCodeScanningFragment.TAG);
            Assert.assertNotNull(fragment);
            Assert.assertTrue(fragment instanceof QRCodeScanningFragment);

            PoPApplication app = (PoPApplication) a.getApplication();
            final String LAO_ID = app.getCurrentLaoUnsafe().getId();
            final String WITNESS_ID = "t9Ed+TEwDM0+u0ZLdS4ZB/Vrrnga0Lu2iMkAQtyFRrQ=";
            final String TEST_IDS = WITNESS_ID + LAO_ID;
            app.addWitness(WITNESS_ID);

            ((QRCodeScanningFragment) fragment).onQRCodeDetected(TEST_IDS, ADD_WITNESS, null);

            for (Lao lao : app.getLaos()) {
                if (lao.getId().equals(LAO_ID)) {
                    List<String> witnesses = lao.getWitnesses();
                    Assert.assertThat(WITNESS_ID, isIn(witnesses));
                }
            }
        });

        onView(withText(getApplicationContext().getString(R.string.add_witness_already_exists)))
                .inRoot(withDecorView(not(decorView)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void canLaunchCreateMeetingEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.meeting_event))).perform(click());
        onView(withId(R.id.fragment_meeting_event_creation)).check(matches(isDisplayed()));
    }

    @Test
    public void canLaunchCreateRollCallEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.roll_call_event))).perform(click());
        onView(withId(R.id.fragment_create_roll_call_event)).check(matches(isDisplayed()));
    }

    @Test
    public void canLaunchCreatePollEventFragment() {
        onView(allOf(withId(R.id.add_future_event_button),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(click());
        onView(withText(getApplicationContext().getString(R.string.poll_event))).check(matches(isDisplayed()));
        onView(withText(getApplicationContext().getString(R.string.poll_event))).perform(click());
        onView(withId(R.id.fragment_organizer_poll)).check(matches(isDisplayed()));
    }
}
