package com.github.dedis.popstellar.testutils.pages.digitalcash;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

public class IssuePageObject {

  private IssuePageObject() {
    throw new IllegalStateException("Page object");
  }

  @IdRes
  public static int fragmentDigitalCashIssueId() {
    return R.id.fragment_digital_cash_issue;
  }
}
