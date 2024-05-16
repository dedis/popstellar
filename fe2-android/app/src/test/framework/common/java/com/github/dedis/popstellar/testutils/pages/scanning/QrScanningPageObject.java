package com.github.dedis.popstellar.testutils.pages.scanning;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class QrScanningPageObject {

  public static ViewInteraction manualAddConfirm() {
    return onView(withId(R.id.manual_add_button));
  }

  public static ViewInteraction openManualButton() {
    return onView(withId(R.id.scanner_enter_manually));
  }

  public static ViewInteraction attendeeCount() {
    return onView(withId(R.id.scanned_number));
  }

  public static ViewInteraction closeManualButton() {
    return onView(withId(R.id.add_manual_close));
  }

  public static ViewInteraction manualInputWithHintRes(@IdRes int hintRes) {
    return onView(withHint(hintRes));
  }

  public static ViewInteraction getPasteFromClipboardButton() {
    return onView(withId(R.id.paste_button));
  }
}
