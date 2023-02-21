package com.github.dedis.popstellar.testutils.pages.lao.token;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class TokenListPageObject {

  public static ViewInteraction validTokenCard() {
    return onView(withId(R.id.valid_token_layout));
  }

  public static ViewInteraction invalidTokensRv() {
    return onView(withId(R.id.tokens_recycler_view));
  }

  public static ViewInteraction emptyTokenText() {
    return onView(withId(R.id.empty_token_text));
  }
}
