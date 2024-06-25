package com.github.dedis.popstellar.testutils.pages.lao.digitalcash;

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


  // create a function for all these elements :     // Click on the first transaction
    //    onView(withId(HistoryPageObject.transactionCardView()))
    //      .perform(ViewActions.click())
    //
    //    // Check if the transaction details are displayed
    //    onView(withId(HistoryPageObject.transactionProvenanceTitle())
    //    )
    //      .check(matches(isDisplayed()))
    //    onView(withId(HistoryPageObject.transactionProvenanceValue())
    //    )
    //      .check(matches(isDisplayed()))
    //    onView(withId(HistoryPageObject.transactionIdValue())
    //    )

    @IdRes
    public static int transactionCardView() {
      return R.id.transaction_card_view;
    }

    @IdRes
    public static int transactionProvenanceTitle() {
      return R.id.history_transaction_provenance_title;
    }

    @IdRes
    public static int transactionProvenanceValue() {
      return R.id.history_transaction_provenance_value;
    }

    @IdRes
    public static int transactionIdValue() {
      return R.id.history_transaction_transaction_id_value;
    }

    @IdRes
    public static int transactionIdTitle() {
        return R.id.history_transaction_transaction_id_title;
    }
}
