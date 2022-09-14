package com.github.dedis.popstellar;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;

/**
 * JUnit Runner provider the HiltTestApplication that will inject dependencies into the tested code.
 *
 * <p>All tests using hilt to inject dependencies should be annotated with {@link HiltAndroidTest}
 *
 * <p>For more information : <a
 * href="https://developer.android.com/training/dependency-injection/hilt-testing">https://developer.android.com/training/dependency-injection/hilt-testing</a>
 */
// This is automatically used when running instrumented tests
@SuppressWarnings("unused")
public final class HiltTestRunner extends AndroidJUnitRunner {

  @Override
  public Application newApplication(ClassLoader cl, String className, Context context)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    return super.newApplication(cl, HiltTestApplication.class.getName(), context);
  }
}
