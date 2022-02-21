package com.github.dedis.popstellar.ui.pages.detail;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;

/**
 * Page object of {@link LaoDetailActivity}
 *
 * <p>Creation : 04/12/2021
 */
public class LaoDetailPageObject {

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_lao_detail));
  }

  public static ViewInteraction homeButton() {
    return onView(withId(R.id.tab_home));
  }

  public static ViewInteraction identityButton() {
    return onView(withId(R.id.tab_identity));
  }

  public static ViewInteraction showPropertiesButton() {
    return onView(withId(R.id.tab_properties));
  }

  public static ViewInteraction propertiesLayout() {
    return onView(withId(R.id.properties_linear_layout));
  }

  public static ViewInteraction connectQrCode() {
    return onView(withId(R.id.channel_qr_code));
  }

  public static ViewInteraction witnessButton() {
    return onView(withId(R.id.tab_witness_message_button));
  }

  @IdRes
  public static int identityFragmentId() {
    return R.id.fragment_identity;
  }

  @IdRes
  public static int witnessFragmentId() {
    return R.id.fragment_witness_message;
  }

  public static String laoIdExtra() {
    return "LAO_ID";
  }

  public static String fragmentToOpenExtra() {
    return "FRAGMENT_TO_OPEN";
  }

  public static String laoDetailValue() {
    return "LaoDetail";
  }
}
