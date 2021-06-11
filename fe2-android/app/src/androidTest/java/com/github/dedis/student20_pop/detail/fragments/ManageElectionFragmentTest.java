package com.github.dedis.student20_pop.detail.fragments;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.utility.network.IdGenerator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;

public class ManageElectionFragmentTest {

    private final String organizer = "kdwaN5WgQhKuDk89H-EcPM58H_lDubHC_7bUHI-_ix0=";
    private final String name = "Lao Name";
    private final long creation = Instant.now().getEpochSecond();
    private final String id = IdGenerator.generateLaoId(organizer, creation, name);
    private final Lao lao = new Lao (id,name);
/*
    private final int YEAR = 2022;
      private final int MONTH_OF_YEAR = 10;
      private final int DAY_OF_MONTH = 10;
      private final String DATE = "" + DAY_OF_MONTH + "/" + MONTH_OF_YEAR + "/" + YEAR;
      private final int HOURS = 12;
      private final int MINUTES = 15;
      private final String TIME = "" + HOURS + ":" + MINUTES;
*/

      @Rule
      public ActivityScenarioRule<HomeActivity> activityScenarioRule =
          new ActivityScenarioRule<>(HomeActivity.class);

      private View decorView;

      @Before
      public void setUp() {
        activityScenarioRule
            .getScenario()
            .onActivity(
                new ActivityScenario.ActivityAction<HomeActivity>() {

                  /**
                   * This method is invoked on the main thread with the reference to the Activity.
                   *
                   * @param homeActivity an Activity instrumented by the {@link ActivityScenario}. It
     never
                   *     be null.
                   */
                  @Override
                  public void perform(HomeActivity homeActivity) {
                    decorView = homeActivity.getWindow().getDecorView();
                    HomeViewModel homeViewModel = HomeActivity.obtainViewModel(homeActivity);
                    homeViewModel.openLAO(id);

                  }
                });

        onView(
                allOf(
                    withId(R.id.add_future_event_button), // here we find the  button to add new  election and click
                                                          // on it
                    withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
            .perform(click());
          onView(
                  allOf(
                          withId(R.id.add_future_event_button), // here we find the  button to add new  election and click
                          // on it
                          withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                  .perform(click());

      }

      @Test
      public void canLaunchManageElectionFragment() {
        onView(withId(R.id.fragment_manage_election)).check(matches((isDisplayed())));
      }


      @Test
      public void cancelButtonWorks() {
        onView(withId(R.id.terminate_election)).perform(click());

      }


}
