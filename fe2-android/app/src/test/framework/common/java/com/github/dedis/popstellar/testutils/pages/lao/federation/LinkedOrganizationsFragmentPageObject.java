package com.github.dedis.popstellar.testutils.pages.lao.federation;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

public class LinkedOrganizationsFragmentPageObject {

    private LinkedOrganizationsFragmentPageObject() {
        throw new IllegalStateException("Page object");
    }

    public static ViewInteraction createLinkButton() {
        return onView(withId(R.id.add_linked_organization));
    }

    public static ViewInteraction inviteButton() {
        return onView(withId(R.id.invite_other_organization));
    }

    public static ViewInteraction joinInvitationButton() {
        return onView(withId(R.id.join_other_organization_invitation));
    }
}
