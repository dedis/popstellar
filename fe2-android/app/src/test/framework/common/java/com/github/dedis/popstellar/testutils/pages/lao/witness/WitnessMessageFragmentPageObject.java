package com.github.dedis.popstellar.testutils.pages.lao.witness;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.ViewInteraction;
import com.github.dedis.popstellar.R;

public class WitnessMessageFragmentPageObject {

  private WitnessMessageFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction witnessMessageList() {
    return onView(withId(R.id.witness_message_list));
  }
  
}
