package com.github.dedis.popstellar.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.UITestUtils.assertToastIsDisplayedWithText
import com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText
import com.github.dedis.popstellar.testutils.pages.home.HomePageObject
import com.github.dedis.popstellar.testutils.pages.home.LaoCreatePageObject
import com.github.dedis.popstellar.testutils.pages.lao.socialmedia.SocialMediaHomePageObject
import com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LaoCreateFragmentTest {

  @BindValue @Mock lateinit var repository: LAORepository

  @BindValue @Mock lateinit var keyManager: KeyManager

  @BindValue @Mock lateinit var messageSender: MessageSender

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(UnknownLaoException::class)
      override fun before() {
        hiltRule.inject()
        Mockito.`when`(repository.allLaoIds)
          .thenReturn(BehaviorSubject.createDefault(listOf(LAO.id)))
        Mockito.`when`(repository.getLaoView(ArgumentMatchers.anyString())).thenReturn(LaoView(LAO))
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

  @Before
  fun setup() {
    // Open the launch tab
    HomeActivityTest.initializeWallet(activityScenarioRule)
    HomePageObject.createButton().perform(ViewActions.click())
  }

  @Test
  fun uiElementsAreCorrectlyDisplayed() {
    LaoCreatePageObject.laoNameEntry().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.serverNameEntry().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    LaoCreatePageObject.clearButtonLaunch()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.confirmButtonLaunch()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.witnessingSwitch().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun uiElementsAreCorrectlyHidden() {
    // The witnessing switch is disabled by default
    LaoCreatePageObject.witnessingSwitch()
      .check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
    LaoCreatePageObject.addWitnessButton()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    LaoCreatePageObject.witnessList()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    LaoCreatePageObject.witnessTitle()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  @Test
  fun confirmButtonGoesToLaoDetail() {
    Intents.init()

    LaoCreatePageObject.laoNameEntry().perform(ViewActions.replaceText(LAO_NAME))
    LaoCreatePageObject.serverNameEntry().perform(ViewActions.replaceText(SERVER_URL))
    LaoCreatePageObject.confirmButtonLaunch()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.confirmButtonLaunch()
      .check(ViewAssertions.matches(ViewMatchers.isEnabled()))

    LaoCreatePageObject.confirmButtonLaunch().perform(ViewActions.click())

    Intents.intended(IntentMatchers.hasComponent(ConnectingActivity::class.java.name))
    Intents.release()
  }

  @Test
  fun clearButtonEraseFilledFields() {
    LaoCreatePageObject.laoNameEntry().perform(ViewActions.replaceText(LAO_NAME))
    LaoCreatePageObject.serverNameEntry().perform(ViewActions.replaceText(SERVER_URL))
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.clearButtonLaunch().perform(ViewActions.click())

    LaoCreatePageObject.laoNameEntry().check(ViewAssertions.matches(ViewMatchers.withText("")))
    LaoCreatePageObject.serverNameEntry().check(ViewAssertions.matches(ViewMatchers.withText("")))
    LaoCreatePageObject.witnessingSwitch()
      .check(ViewAssertions.matches(ViewMatchers.isNotSelected()))
    LaoCreatePageObject.witnessingSwitch()
      .check(
        ViewAssertions.matches(ViewMatchers.withText(R.string.lao_create_enable_witnessing_switch))
      )
    LaoCreatePageObject.addWitnessButton()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    LaoCreatePageObject.witnessList()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    LaoCreatePageObject.witnessTitle()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  @Test
  fun addWitnessButtonGoesToScanner() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())
    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    HomePageObject.fragmentContainer()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withChild(ViewMatchers.withId(HomePageObject.qrScannerFragmentId()))
        )
      )
  }

  @Test
  fun witnessingSwitchDisplayButton() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())
    LaoCreatePageObject.addWitnessButton().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun witnessingSwitchEnabledPopulateWitnesses() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())
    Intents.init()

    LaoCreatePageObject.laoNameEntry().perform(ViewActions.replaceText(LAO_NAME))
    LaoCreatePageObject.serverNameEntry().perform(ViewActions.replaceText(SERVER_URL))

    LaoCreatePageObject.confirmButtonLaunch()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.confirmButtonLaunch()
      .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
    LaoCreatePageObject.confirmButtonLaunch().perform(ViewActions.click())

    Intents.intended(IntentMatchers.hasComponent(ConnectingActivity::class.java.name))
    Intents.intended(IntentMatchers.hasExtra(Constants.WITNESSING_FLAG_EXTRA, true))
    Intents.release()
  }

  @Test
  fun addingWitnessShowTitleAndKeys() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())

    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(VALID_WITNESS_MANUAL_INPUT))

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.closeManualButton().perform(ViewActions.click())

    SocialMediaHomePageObject.getRootView().perform(ViewActions.pressBack())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    LaoCreatePageObject.witnessList().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.witnessTitle().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    assertToastIsDisplayedWithText(R.string.witness_scan_success)
  }

  @Test
  fun addingEmptyKeyDoesNotAddWitness() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())

    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(EMPTY_KEY_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    assertToastIsDisplayedWithText(R.string.qrcode_scanning_manual_entry_error)

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.closeManualButton().perform(ViewActions.click())

    SocialMediaHomePageObject.getRootView().perform(ViewActions.pressBack())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    LaoCreatePageObject.witnessList().check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    LaoCreatePageObject.witnessTitle().check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
  }

  @Test
  fun addingInvalidKeyDoesNotAddWitness() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())

    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(INVALID_KEY_FORMAT_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    assertToastIsDisplayedWithText(R.string.qr_code_not_main_pk)

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.closeManualButton().perform(ViewActions.click())

    SocialMediaHomePageObject.getRootView().perform(ViewActions.pressBack())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    LaoCreatePageObject.witnessList().check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    LaoCreatePageObject.witnessTitle().check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
  }

  @Test
  fun addingWitnessTwiceDoesNotIncreaseCount() {
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())

    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(VALID_WITNESS_MANUAL_INPUT))

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.closeManualButton().perform(ViewActions.click())
    SocialMediaHomePageObject.getRootView().perform(ViewActions.pressBack())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    LaoCreatePageObject.witnessList().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.witnessTitle().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    assertToastIsDisplayedWithText(R.string.witness_scan_success)

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    input.perform(forceTypeText(VALID_WITNESS_MANUAL_INPUT))

    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    assertToastIsDisplayedWithText(R.string.witness_already_scanned_warning)
  }

  @Test
  fun addPasteFromClipboardTest(){
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())

    val clipboard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(
      Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", VALID_WITNESS_MANUAL_INPUT))
    QrScanningPageObject.getPasteFromClipboardButton().perform(ViewActions.click())
    QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint).check(ViewAssertions.matches(ViewMatchers.withText(VALID_WITNESS_MANUAL_INPUT)))

    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    QrScanningPageObject.closeManualButton().perform(ViewActions.click())

    SocialMediaHomePageObject.getRootView().perform(ViewActions.pressBack())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    LaoCreatePageObject.witnessList().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    LaoCreatePageObject.witnessTitle().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun addPasteFromClipboardWithEmptyClipboardTest(){
    LaoCreatePageObject.witnessingSwitch().perform(ViewActions.click())

    LaoCreatePageObject.addWitnessButton().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())

    QrScanningPageObject.getPasteFromClipboardButton().perform(ViewActions.click())
    QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint).check(ViewAssertions.matches(ViewMatchers.withText("")))

    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
    assertToastIsDisplayedWithText(R.string.qrcode_scanning_manual_entry_error)
  }

  companion object {
    private const val LAO_NAME = "LAO"
    private const val SERVER_URL = "localhost"
    private val KEY_PAIR = Base64DataUtils.generateKeyPair()
    private val PK = KEY_PAIR.publicKey
    private val LAO = Lao(LAO_NAME, PK, 10223421)
    private val EMPTY_KEY_INPUT = ""
    private val INVALID_KEY_FORMAT_INPUT = "invalid for sure"
    val VALID_WITNESS_MANUAL_INPUT = PK.encoded
  }
}
