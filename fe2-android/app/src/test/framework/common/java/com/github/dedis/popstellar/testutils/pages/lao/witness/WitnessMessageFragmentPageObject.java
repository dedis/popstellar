package com.github.dedis.popstellar.testutils.pages.lao.witness;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;
import androidx.test.espresso.ViewInteraction;
import com.github.dedis.popstellar.R;
import org.hamcrest.Matcher;

public class WitnessMessageFragmentPageObject {

  private WitnessMessageFragmentPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static Matcher<View> witnessMessageListMatcher() {
    return withId(R.id.witness_message_list);
  }

  public static ViewInteraction witnessMessageList() {
    return onView(witnessMessageListMatcher());
  }

  public static Matcher<View> messageDescriptionArrowMatcher() {
    return withId(R.id.message_description_arrow);
  }

  public static Matcher<View> messageSignaturesArrowMatcher() {
    return withId(R.id.signatures_arrow);
  }
  
  public static ViewInteraction messageDescriptionText() {
    return onView(withId(R.id.message_description_text));
  }

  public static ViewInteraction witnessSignaturesText() {
    return onView(withId(R.id.witnesses_text));
  }

  public static ViewInteraction witnessMessageFragment() {
    return onView(withId(R.id.fragment_witness_message));
  }
}
