package com.github.dedis.popstellar.testutils.pages.detail.event.rollcall;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class RollCallCreatePageObject {

  public static ViewInteraction rollCallCreateTitle() {
    return onView(withId(R.id.roll_call_title_text));
  }

  public static ViewInteraction rollCallCreateConfirmButton() {
    return onView(withId(R.id.roll_call_confirm));
  }

  public static ViewInteraction rollCreateOpenButton() {
    return onView(withId(R.id.roll_call_open));
  }
}
