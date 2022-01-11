package com.github.dedis.popstellar.pages.qrcode;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class QRCodeScanningPageObject {

  public static ViewInteraction addAttendeeButton() {
    return onView(
        allOf(withId(R.id.add_attendee_manually), isClickable(), isEnabled(), isDisplayed()));
  }

  public static ViewInteraction closeRollCallButton() {
    return onView(
        allOf(withId(R.id.add_attendee_confirm), isClickable(), isEnabled(), isDisplayed()));
  }

  public static ViewInteraction addAttendeeTokenTextInput() {
    return onView(
        allOf(withHint(R.string.add_attendee_hint), isClickable(), isEnabled(), isDisplayed()));
  }

  public static ViewInteraction cancelButton() {
    return onView(allOf(withText(R.string.cancel), isClickable(), isEnabled(), isDisplayed()));
  }

  public static ViewInteraction confirmButton() {
    return onView(allOf(withText(R.string.confirm), isClickable(), isEnabled(), isDisplayed()));
  }

  public static ViewInteraction successPopup() {
    return onView(withText(R.string.attendee_added));
  }

  public static ViewInteraction invalidTokenPopup() {
    return onView(withText(R.string.invalid_token));
  }

  public static ViewInteraction tokenAlreadyAddedPopup() {
    return onView(withText(R.string.qrcode_already_scanned));
  }

  public static ViewInteraction okButton() {
    return onView(allOf(withText(R.string.ok), isClickable(), isEnabled(), isDisplayed()));
  }
}
