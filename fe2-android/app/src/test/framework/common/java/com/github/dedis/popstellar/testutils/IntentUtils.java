package com.github.dedis.popstellar.testutils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

public class IntentUtils {

  private IntentUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static Intent createIntent(
      Class<? extends Activity> activityClass, @NonNull Bundle extras) {
    Intent intent = new Intent(ApplicationProvider.getApplicationContext(), activityClass);
    intent.putExtras(extras);
    return intent;
  }
}
