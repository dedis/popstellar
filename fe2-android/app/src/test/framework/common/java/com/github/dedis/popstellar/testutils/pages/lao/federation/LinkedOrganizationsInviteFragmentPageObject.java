package com.github.dedis.popstellar.testutils.pages.lao.federation;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

public class LinkedOrganizationsInviteFragmentPageObject {

    private LinkedOrganizationsInviteFragmentPageObject() {
        throw new IllegalStateException("Page object");
    }

    public static ViewInteraction scanQrText() {
        return onView(withId(R.id.scan_qr_text));
    }

    public static ViewInteraction qrCode() {
        return onView(withId(R.id.federation_qr_code));
    }

    public static ViewInteraction organizationName() {
        return onView(withId(R.id.linked_organizations_name_text));
    }

    public static ViewInteraction nextStepButton() {
        return onView(withId(R.id.next_step_button));
    }
}
