package com.github.dedis.popstellar.ui;

import androidx.annotation.StringRes;

public interface PopViewModel {
  void setPageTitle(@StringRes int title);

  String getLaoId();
}
