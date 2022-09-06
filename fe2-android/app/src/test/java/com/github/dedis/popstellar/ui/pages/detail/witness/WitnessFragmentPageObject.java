package com.github.dedis.popstellar.ui.pages.detail.witness;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class WitnessFragmentPageObject {

  public static ViewInteraction addWitnessButton() {
    return onView(withId(R.id.add_witness_button));
  }
}
