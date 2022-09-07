package com.github.dedis.popstellar.testutils.pages.detail.event;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class EventCreationPageObject {

  private EventCreationPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction startDateView() {
    return onView(withId(R.id.start_date_edit_text));
  }

  public static ViewInteraction endDateView() {
    return onView(withId(R.id.end_date_edit_text));
  }

  public static ViewInteraction startTimeView() {
    return onView(withId(R.id.start_time_edit_text));
  }

  public static ViewInteraction endTimeView() {
    return onView(withId(R.id.end_time_edit_text));
  }
}
