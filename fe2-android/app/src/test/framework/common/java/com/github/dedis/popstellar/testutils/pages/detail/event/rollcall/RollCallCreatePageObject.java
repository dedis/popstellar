package com.github.dedis.popstellar.testutils.pages.detail.event.rollcall;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class RollCallCreatePageObject {

  private RollCallCreatePageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction rollCallCreateTitle() {
    return onView(withId(R.id.roll_call_title_text));
  }

  public static ViewInteraction rollCallCreateLocation() {
    return onView(withId(R.id.roll_call_event_location_text));
  }

  public static ViewInteraction rollCallCreateConfirmButton() {
    return onView(withId(R.id.roll_call_confirm));
  }
}
