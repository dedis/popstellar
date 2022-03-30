package com.github.dedis.popstellar.ui.pages.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class HomeFragmentPageObject {
  public static ViewInteraction laoList() {
    return onView(withId(R.id.lao_list));
  }
}
