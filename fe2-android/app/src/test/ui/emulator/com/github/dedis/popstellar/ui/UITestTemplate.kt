package com.github.dedis.popstellar.ui

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.ui.home.HomeActivity
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

/**
 * This test class is a template that you can use as a basis to write new UI tests.
 *
 * First of all, these annotations are needed :
 * * [HiltAndroidTest] is needed because we are instantiating Activities and Fragments that needs
 *   Hilt's service injection
 * * [RunWith] is needed by the Robolectric tests, but is also work in the emulator tests so keep
 *   it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UITestTemplate {
  /**
   * This defines a [org.junit.rules.TestRule] which encapsulate setup and teardown behaviors of the
   * the suite
   *
   * This particular rule automatically mocks the fields annotated with [Mock] at the start of each
   * test using the Mockito library.
   */
  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  /**
   * This rule is necessary to use Hilt service injection.
   *
   * It has also two other feature:
   * * It can inject services in the test fields using the [Inject] annotation.
   *
   * An example of the is the Gson service injected in the gson field below.
   * * It can replace existing injection rule with ones defined by the test using [BindValue]
   *
   * For more information about the use of Hilt in test, it is advised to read the related
   * [documentation](https://developer.android.com/training/dependency-injection/hilt-testing)
   */
  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  /**
   * When creating a UI test, the tested ui element is started. And to do that, it needs some
   * functionality mocked.
   *
   * Therefore, the mocking needs to be done before the ui rule is executed. The solution is this
   * [ExternalResource] rule. We can use its before function to create the mocks.
   */
  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(UnknownLaoException::class)
      override fun before() {
        // This line is needed by the hilt rule to inject the fields annotated with @inject
        hiltRule.inject()
        // Place the mock definition here
        Mockito.`when`(laoRepo.getLaoView(ArgumentMatchers.anyString()))
          .thenReturn(null) // Put the LaoView object in the return
      }
    }

  /**
   * The rule responsible of starting the activity. If you want to start a Fragment, take a look at
   * [com.github.dedis.popstellar.testutils.fragment.FragmentScenarioRule] and [ ]
   */
  @JvmField @Rule(order = 3) val activityRule = ActivityScenarioRule(HomeActivity::class.java)

  // This field is injected by hilt and will contain the application Gson
  @Inject lateinit var gson: Gson

  // This field is a mock of an LAORepository that is instantiated by the mockito rule. It i then
  // bound as the LAORepository service for the app.
  @Mock @BindValue lateinit var laoRepo: LAORepository

  @Test
  fun theTest() {
    // Place your test code here
  }
}
