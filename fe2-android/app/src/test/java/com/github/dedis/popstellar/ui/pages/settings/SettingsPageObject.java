package com.github.dedis.popstellar.ui.pages.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class SettingsPageObject {

  public static ViewInteraction pageTitle() {
    return onView(withId(R.id.server_url_title));
  }

  public static ViewInteraction serverUrlEditText() {
    return onView(withId(R.id.entry_box_server_url));
  }

  public static ViewInteraction applyButton() {
    return onView(withId(R.id.button_apply));
  }
}
