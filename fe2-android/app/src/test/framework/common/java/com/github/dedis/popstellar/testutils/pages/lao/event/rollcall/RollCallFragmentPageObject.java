package com.github.dedis.popstellar.testutils.pages.lao.event.rollcall;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class RollCallFragmentPageObject {

  private RollCallFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction rollCallTitle() {
    return onView(withId(R.id.roll_call_fragment_title));
  }

  public static ViewInteraction rollCallStatusText() {
    return onView(withId(R.id.roll_call_status));
  }

  public static ViewInteraction rollCallStartTime() {
    return onView(withId(R.id.roll_call_start_time));
  }

  public static ViewInteraction rollCallEndTime() {
    return onView(withId(R.id.roll_call_end_time));
  }

  public static ViewInteraction managementButton() {
    return onView(withId(R.id.roll_call_management_button));
  }

  public static ViewInteraction rollCallScanButton() {
    return onView(withId(R.id.roll_call_scanning_button));
  }
}
