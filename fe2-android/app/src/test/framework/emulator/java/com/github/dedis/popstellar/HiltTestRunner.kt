package com.github.dedis.popstellar

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * JUnit Runner provider the HiltTestApplication that will inject dependencies into the tested code.
 *
 *
 * All tests using hilt to inject dependencies should be annotated with [HiltAndroidTest]
 *
 *
 * For more information : [https://developer.android.com/training/dependency-injection/hilt-testing](https://developer.android.com/training/dependency-injection/hilt-testing)
 */
// This is automatically used when running instrumented tests
@Suppress("unused")
class HiltTestRunner : AndroidJUnitRunner() {
  @Throws(
    ClassNotFoundException::class,
    IllegalAccessException::class,
    InstantiationException::class
  )
  override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
    return super.newApplication(cl, HiltTestApplication::class.java.name, context)
  }
}