package com.github.dedis.popstellar.ui.pages.detail.event.rollcall;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class RollCallFragmentPageObject {

  public static ViewInteraction rollCallTitle() {
    return onView(withId(R.id.roll_call_fragment_title));
  }

  public static ViewInteraction rollCallStatusText() {
    return onView(withId(R.id.roll_call_fragment_status));
  }

  public static ViewInteraction rollCallStartTime() {
    return onView(withId(R.id.roll_call_fragment_start_time));
  }

  public static ViewInteraction rollCallEndTime() {
    return onView(withId(R.id.roll_call_fragment_end_time));
  }

  public static ViewInteraction managementButton() {
    return onView(withId(R.id.roll_call_management_button));
  }
}
