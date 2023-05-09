package com.github.dedis.popstellar.testutils.pages.home;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class QrPageObject {
  private QrPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction qrCode() {
    return onView(withId(R.id.pk_qr_code));
  }

  public static ViewInteraction privateKey() {
    return onView(withId(R.id.pk_text));
  }
}
