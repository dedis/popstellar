package com.github.dedis.popstellar.pages.detail;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

public class LaoDetailPageObject {

  @IdRes
  public static int fragmentContainer() {
    return R.id.fragment_container_lao_detail;
  }

  public static String laoIdExtra() {
    return "LAO_ID";
  }

  public static String fragmentToOpenExtra() {
    return "FRAGMENT_TO_OPEN";
  }

  public static String laoDetailValue() {
    return "LaoDetail";
  }
}
