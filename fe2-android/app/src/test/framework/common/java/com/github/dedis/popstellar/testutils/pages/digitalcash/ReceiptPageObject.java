package com.github.dedis.popstellar.testutils.pages.digitalcash;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

public class ReceiptPageObject {

  private ReceiptPageObject() {
    throw new IllegalStateException("Page object");
  }

  @IdRes
  public static int fragmentDigitalCashReceiptId() {
    return R.id.fragment_digital_cash_receipt;
  }
}
