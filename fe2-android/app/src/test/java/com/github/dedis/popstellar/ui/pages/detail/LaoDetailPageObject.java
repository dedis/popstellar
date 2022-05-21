package com.github.dedis.popstellar.ui.pages.detail;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.Constants;

/**
 * Page object of {@link LaoDetailActivity}
 *
 * <p>Creation : 04/12/2021
 */
public class LaoDetailPageObject {

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_lao_detail));
  }

  public static ViewInteraction identityButton() {
    return onView(withId(R.id.lao_detail_identity_menu));
  }

  public static ViewInteraction qrCodeIcon() {
    return onView(withId(R.id.qr_code_icon));
  }

  public static ViewInteraction qrCodeLayout() {
    return onView(withId(R.id.lao_detail_qr_layout));
  }

  public static ViewInteraction connectQrCode() {
    return onView(withId(R.id.channel_qr_code));
  }

  public static ViewInteraction witnessButton() {
    return onView(withId(R.id.lao_detail_witnessing_menu));
  }

  @IdRes
  public static int identityFragmentId() {
    return R.id.fragment_identity;
  }

  @IdRes
  public static int witnessingFragmentId() {
    return R.id.fragment_witnessing;
  }


  public static String laoIdExtra() {
    return Constants.LAO_ID_EXTRA;
  }

  public static String fragmentToOpenExtra() {
    return Constants.FRAGMENT_TO_OPEN_EXTRA;
  }

  public static String laoDetailValue() {
    return Constants.LAO_DETAIL_EXTRA;
  }
}
