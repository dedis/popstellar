package com.github.dedis.popstellar.testutils.pages.home;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.home.LaoCreateFragment;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Page object of {@link LaoCreateFragment}
 *
 * <p>Creation : 04.07.2022
 */
public class LaoCreatePageObject {

  private LaoCreatePageObject() {
    throw new IllegalStateException("Page object");
  }

  @IdRes
  public static int createFragmentId() {
    return R.id.fragment_lao_create;
  }

  public static ViewInteraction laoNameEntry() {
    return onView(withId(R.id.lao_name_entry_edit_text));
  }

  public static ViewInteraction serverNameEntry(){
      return onView(withId(R.id.server_url_entry_edit_text));
  }

  public static ViewInteraction cancelButtonLaunch() {
    return onView(withId(R.id.button_cancel_launch));
  }

  public static ViewInteraction confirmButtonLaunch() {
    return onView(withId(R.id.button_create));
  }
}
