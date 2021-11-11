package com.github.dedis.popstellar.testutils;

import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

/**
 * A Mocked ActivityResultRegistry.
 *
 * <p>Use setResultProvider() to provider your own logic result logic for each tests
 */
public class MockResultRegistry extends ActivityResultRegistry {

  private ResultProvider resultProvider;

  @Override
  public <I, O> void onLaunch(
      int requestCode,
      @NonNull ActivityResultContract<I, O> contract,
      I input,
      @Nullable ActivityOptionsCompat options) {
    if (resultProvider != null) dispatchResult(requestCode, resultProvider.get(requestCode));
  }

  /** Override the current provider with the one given */
  public void setResultProvider(ResultProvider resultProvider) {
    this.resultProvider = resultProvider;
  }

  @FunctionalInterface
  public interface ResultProvider {
    Object get(int requestCode);
  }
}
