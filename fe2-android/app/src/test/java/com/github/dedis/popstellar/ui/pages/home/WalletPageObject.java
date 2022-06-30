package com.github.dedis.popstellar.ui.pages.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;


/**
 * Page object of {@Link com.github.dedis.popstellar.ui.home.WalletFragment}
 *
 * <p>Creation : 26.03.2022
 */
public class WalletPageObject {

  @IdRes
  public static int walletFragmentId() {
    return R.id.fragment_wallet;
  }

  @IdRes
  public static int walletSeedFragmentId(){
    return R.id.fragment_seed_wallet;
  }

  @IdRes
  public static int walletContentFragmentId(){
    return R.id.fragment_content_wallet;
  }

  public static ViewInteraction confirmButton() {
    return onView(withId(R.id.button_confirm_seed));
  }

  public static ViewInteraction newWalletButton(){
    return onView(withId(R.id.button_new_wallet));
  }

  public static ViewInteraction iOwnASeedButton(){
    return onView(withId(R.id.button_own_seed));
  }

  public static ViewInteraction logoutButton(){
    return onView(withId(R.id.logout_button));
  }

  public static ViewInteraction welcomeText1(){
    return onView(withId(R.id.wallet_welcome_text_1));
  }

  public static ViewInteraction welcomeText2(){
    return onView(withId(R.id.wallet_welcome_text_2));
  }

  public static ViewInteraction welcomeText3(){
    return onView(withId(R.id.wallet_welcome_text_3));
  }

  public static ViewInteraction walletSeedWarningText(){
    return onView(withId(R.id.wallet_seed_warning_text));
  }

  public static ViewInteraction seedWalletText(){
    return onView(withId(R.id.seed_wallet_text));
  }

  public static ViewInteraction tokenTitle(){
    return onView(withId(R.id.title_wallet));
  }

  public static ViewInteraction walletContentText1(){
    return onView(withId(R.id.wallet_content_text_1));
  }

  public static ViewInteraction walletContentText2(){
    return onView(withId(R.id.wallet_content_text_2));
  }

  public static ViewInteraction logOutButton(){
    return onView(withId(R.id.logout_button));
  }

}
