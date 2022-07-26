package com.github.dedis.popstellar.ui.pages.detail;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.Constants;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@link LaoDetailActivity}
 *
 * <p>Creation : 04/12/2021
 */
public class LaoDetailActivityPageObject {

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_lao_detail));
  }

  public static ViewInteraction identityButton() {
    return onView(withId(R.id.lao_detail_identity_menu));
  }

  public static ViewInteraction socialMediaButton() {
    return onView(withId(R.id.lao_detail_social_media_menu));
  }

  public static ViewInteraction digitalCashButton() {
    return onView(withId(R.id.lao_detail_digital_cash_menu));
  }

  public static ViewInteraction witnessButton() {
    return onView(withId(R.id.lao_detail_witnessing_menu));
  }

  public static ViewInteraction toolBarBackButton() {
    return onView(withContentDescription("Navigate up"));
  }

  @IdRes
  public static int laoDetailFragmentId() {
    return R.id.fragment_lao_detail;
  }

  @IdRes
  public static int identityFragmentId() {
    return R.id.fragment_identity;
  }

  @IdRes
  public static int witnessingFragmentId() {
    return R.id.fragment_witnessing;
  }

  @IdRes
  public static int cameraPermissionId() {
    return R.id.fragment_camera_perm;
  }

  public static int containerId() {
    return R.id.fragment_container_lao_detail;
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
