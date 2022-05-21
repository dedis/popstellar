package com.github.dedis.popstellar.ui.pages.digitalcash;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class SendPageObject {
    @IdRes
    public static int fragmentDigitalCashSendId() {
        return R.id.fragment_digital_cash_send;
    }
}
