package com.github.dedis.popstellar.testutils.pages.home;

import androidx.annotation.StringRes;

import com.github.dedis.popstellar.R;

public class SettingsPageObject {
  private SettingsPageObject() {
    throw new IllegalStateException("Page object");
  }

  @StringRes
  public static int serverUrlKey() {
    return R.string.settings_server_url_key;
  }

  @StringRes
  public static int switchLogging() {
    return R.string.settings_logging_key;
  }
}
