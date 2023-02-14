package com.github.dedis.popstellar.testutils.pages.lao;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.utility.Constants;

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

  public static String laoIdExtra() {
    return Constants.LAO_ID_EXTRA;
  }

  @IdRes
  public static int laoDetailFragmentId() {
    return R.id.fragment_event_list;
  }

  @IdRes
  public static int qrCodeFragmentId() {
    return R.id.fragment_qrcode;
  }
}
