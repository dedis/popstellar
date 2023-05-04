package com.github.dedis.popstellar.testutils.pages.lao.event.rollcall;

import androidx.annotation.IdRes;
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

  public static ViewInteraction rollCallAttendeesText() {
    return onView(withId(R.id.roll_call_attendees_text));
  }

  public static ViewInteraction rollCallListAttendees() {
    return onView(withId(R.id.list_view_attendees));
  }

  public static ViewInteraction rollCallQRCode() {
    return onView(withId(R.id.roll_call_pk_qr_code));
  }

  public static ViewInteraction rollCallLocationCard() {
    return onView(withId(R.id.roll_call_location_card));
  }

  public static ViewInteraction rollCallLocationText() {
    return onView(withId(R.id.roll_call_location_text));
  }

  public static ViewInteraction rollCallDescriptionCard() {
    return onView(withId(R.id.roll_call_description_card));
  }

  public static ViewInteraction rollCallDescriptionText() {
    return onView(withId(R.id.roll_call_description_text));
  }

  public static ViewInteraction rollCallDescriptionAndLocationContainer() {
    return onView(withId(R.id.roll_call_metadata_container));
  }

  @IdRes
  public static int fragmentId() {
    return R.id.fragment_roll_call;
  }
}
