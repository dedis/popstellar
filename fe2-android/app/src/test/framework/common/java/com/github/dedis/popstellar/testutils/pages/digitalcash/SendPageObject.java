package com.github.dedis.popstellar.testutils.pages.digitalcash;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class SendPageObject {

  private SendPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction sendButtonToReceipt() {
    return onView(withId(R.id.digital_cash_send_send));
  }

  public static ViewInteraction sendSpinner() {
    return onView(withId(R.id.digital_cash_send_spinner_tv));
  }

  @IdRes
  public static int fragmentDigitalCashSendId() {
    return R.id.fragment_digital_cash_send;
  }
}
