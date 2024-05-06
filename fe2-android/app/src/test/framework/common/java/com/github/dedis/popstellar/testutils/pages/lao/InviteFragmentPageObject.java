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

  public static ViewInteraction copyIdentifierButton() {
    return onView(withId(R.id.copy_identifier_button));
  }

  public static ViewInteraction copyServerButton() {
      return onView(withId(R.id.copy_server_button));
  }

  public static ViewInteraction serverText() {
    return onView(withId(R.id.lao_properties_server_text));
  }
}


