package com.github.dedis.popstellar.testutils.pages.lao;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class MeetingFragmentPageObject {
  private MeetingFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction meetingTitle() {
    return onView(withId(R.id.meeting_title));
  }

  public static ViewInteraction meetingStatusText() {
    return onView(withId(R.id.meeting_status));
  }

  public static ViewInteraction meetingStatusIcon() {
    return onView(withId(R.id.meeting_status_icon));
  }

  public static ViewInteraction meetingStartTime() {
    return onView(withId(R.id.meeting_start_time));
  }

  public static ViewInteraction meetingEndTime() {
    return onView(withId(R.id.meeting_end_time));
  }

  public static ViewInteraction meetingLocationText() {
    return onView(withId(R.id.meeting_location_text));
  }

  @IdRes
  public static int fragmentId() {
    return R.id.fragment_meeting;
  }
}
