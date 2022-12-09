package com.github.dedis.popstellar.testutils.pages.home;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@link com.github.dedis.popstellar.ui.wallet.SeedWalletFragment}
 *
 * <p>Creation : 26.03.2022
 */
public class WalletPageObject {

  private WalletPageObject() {
    throw new IllegalStateException("Page object");
  }

  @IdRes
  public static int walletId() {
    return R.id.fragment_seed_wallet;
  }

  public static ViewInteraction confirmButton() {
    return onView(withId(R.id.button_confirm_seed));
  }
}
