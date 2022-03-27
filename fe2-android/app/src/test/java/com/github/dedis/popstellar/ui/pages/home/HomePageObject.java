package com.github.dedis.popstellar.ui.pages.home;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;
import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@Link HomeActivity}
 *
 * <p>Creation : 26.03.2021
 */
public class HomePageObject {

  public static ViewInteraction fragmentContainer(){
    return  onView(withId(R.id.fragment_container_home));
  }

  public static ViewInteraction homeButton(){
    return onView(withId(R.id.tab_home));
  }

  public static ViewInteraction connectButton(){
    return onView(withId(R.id.tab_connect));
  }

  public static ViewInteraction launchButton(){
    return onView(withId(R.id.tab_launch));
  }

  public static ViewInteraction walletButton(){
    return onView(withId(R.id.tab_wallet));
  }

  public static ViewInteraction socialMediaButton(){
    return onView(withId(R.id.tab_social_media));
  }

  @IdRes
  public static int homeFragmentId(){
    return R.id.fragment_home;
  }



}
