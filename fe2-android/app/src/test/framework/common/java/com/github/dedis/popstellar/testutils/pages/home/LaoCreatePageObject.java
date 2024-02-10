package com.github.dedis.popstellar.testutils.pages.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.home.LaoCreateFragment;

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

  public static ViewInteraction serverNameEntry() {
    return onView(withId(R.id.server_url_entry_edit_text));
  }

  public static ViewInteraction clearButtonLaunch() {
    return onView(withId(R.id.button_clear_launch));
  }

  public static ViewInteraction confirmButtonLaunch() {
    return onView(withId(R.id.button_create));
  }

  public static ViewInteraction addWitnessButton() {
    return onView(withId(R.id.add_witness_button));
  }

  public static ViewInteraction witnessTitle() {
    return onView(withId(R.id.witnesses_title));
  }

  public static ViewInteraction witnessList() {
    return onView(withId(R.id.witnesses_list));
  }

  public static ViewInteraction witnessingSwitch() {
    return onView(withId(R.id.enable_witnessing_switch));
  }
}
