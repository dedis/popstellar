package com.github.dedis.popstellar.testutils.pages.digitalcash;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

public class HistoryPageObject {

  private HistoryPageObject() {
    throw new IllegalStateException("Page object");
  }

  @IdRes
  public static int fragmentDigitalCashHistoryId() {
    return R.id.fragment_digital_cash_history;
  }
}
