package com.github.dedis.popstellar.ui.pages.digitalcash;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class SendPageObject {
    public static ViewInteraction sendButtonToReceipt() {
        return onView(withId(R.id.digital_cash_send_send));
    }

    @IdRes
    public static int fragmentDigitalCashSendId() {
        return R.id.fragment_digital_cash_send;
    }
}
