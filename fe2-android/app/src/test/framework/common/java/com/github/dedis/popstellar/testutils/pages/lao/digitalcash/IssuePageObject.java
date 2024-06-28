package com.github.dedis.popstellar.testutils.pages.lao.digitalcash;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.annotation.IdRes;
import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

public class IssuePageObject {

  private IssuePageObject() {
    throw new IllegalStateException("Page object");
  }

  @IdRes
  public static int fragmentDigitalCashIssueId() {
    return R.id.fragment_digital_cash_issue;
  }

    public static ViewInteraction issueButton() {
      return onView(withId(R.id.digital_cash_issue_issue));
    }

    public static ViewInteraction issueAmount() {
      return onView(withId(R.id.digital_cash_issue_amount));
    }

    public static ViewInteraction radioButtonAttendees() {
      return onView(withId(R.id.radioButtonAttendees));
    }

    public static ViewInteraction spinner() {
      return onView(withId(R.id.digital_cash_issue_spinner_tv));
    }
}
