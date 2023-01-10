package com.github.dedis.popstellar.testutils.pages.detail;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.Constants;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@link LaoDetailActivity}
 *
 * <p>Creation : 04/12/2021
 */
public class LaoDetailActivityPageObject {

  private LaoDetailActivityPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction fragmentContainer() {
    return onView(withId(R.id.fragment_container_lao_detail));
  }

  public static ViewInteraction socialMediaButton() {
    return onView(withId(R.id.main_menu_digital_cash));
  }

  public static ViewInteraction digitalCashButton() {
    return onView(withId(R.id.main_menu_digital_cash));
  }

  @IdRes
  public static int digitalCashMenu() {
    return R.id.main_menu_digital_cash;
  }

  public static ViewInteraction witnessButton() {
    return onView(withId(R.id.main_menu_witnessing));
  }

  public static ViewInteraction drawerLayout() {
    return onView(withId(R.id.lao_detail_drawer_layout));
  }

  public static ViewInteraction navigationDrawer() {
    return onView(withId(R.id.lao_detail_navigation_drawer));
  }

  @IdRes
  public static int laoDetailFragmentId() {
    return R.id.fragment_lao_detail;
  }

  @IdRes
  public static int witnessingFragmentId() {
    return R.id.fragment_witnessing;
  }

  @IdRes
  public static int qrCodeFragmentId() {
    return R.id.fragment_qrcode;
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
