package com.github.dedis.popstellar.testutils.pages.detail.event.pickers;

import com.github.dedis.popstellar.ui.lao.event.pickers.PickerConstant;

public class TimePickerPageObject {

  private TimePickerPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static String getRequestKey() {
    return PickerConstant.REQUEST_KEY;
  }

  public static String getBundleResponseKey() {
    return PickerConstant.RESPONSE_KEY;
  }
}
