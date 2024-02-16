package com.github.dedis.popstellar.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.pages.home.HomePageObject
import com.github.dedis.popstellar.testutils.pages.home.QrPageObject
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QrFragmentTest {
  @BindValue @Mock lateinit var keyManager: KeyManager

  private lateinit var gson: Gson

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      override fun before() {
        hiltRule.inject()
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(PK)
        Mockito.`when`(keyManager.mainKeyPair).thenReturn(KEY_PAIR)
        gson = provideGson(buildRegistry())
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

  @Before
  fun setup() {
    // Open the launch tab
    HomeActivityTest.initializeWallet(activityScenarioRule)
    HomePageObject.witnessButton().perform(ViewActions.click())
  }

  @Test
  fun elementsAreDisplayedTest() {
    QrPageObject.qrCode().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    QrPageObject.privateKey().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun publicKeyIsCorrectTest() {
    QrPageObject.privateKey()
      .check(ViewAssertions.matches(ViewMatchers.withText(keyManager.mainPublicKey.encoded)))
  }

  companion object {
    private val KEY_PAIR = Base64DataUtils.generateKeyPair()
    private val PK = KEY_PAIR.publicKey
  }
}
