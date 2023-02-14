package com.github.dedis.popstellar.testutils.pages.lao;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class LaoActivityPageObject {

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_lao));
  }

  @IdRes
  public static int containerId() {
    return R.id.fragment_container_lao;
  }
}
