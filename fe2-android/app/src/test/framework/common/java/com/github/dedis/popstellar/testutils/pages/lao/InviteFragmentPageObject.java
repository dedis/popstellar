package com.github.dedis.popstellar.testutils.pages.lao;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class InviteFragmentPageObject {

  private InviteFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction roleText() {
    return onView(withId(R.id.lao_properties_role_text));
  }

  public static ViewInteraction identifierText() {
    return onView(withId(R.id.lao_properties_identifier_text));
  }

  public static ViewInteraction laoNameText() {
    return onView(withId(R.id.lao_properties_name_text));
  }

  public static ViewInteraction channelQRCode() {
    return onView(withId(R.id.channel_qr_code));
  }
}
