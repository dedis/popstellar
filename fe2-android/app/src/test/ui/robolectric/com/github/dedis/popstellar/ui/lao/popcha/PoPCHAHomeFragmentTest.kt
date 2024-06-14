package com.github.dedis.popstellar.ui.lao.popcha

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.RollCall.Companion.generateCreateRollCallId
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.AuthToken
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.UITestUtils
import com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.popcha.PoPCHAHomePageObject
import com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PoPCHAHomeFragmentTest {
  @Inject lateinit var rollCallRepository: RollCallRepository

  @BindValue @Mock lateinit var laoRepo: LAORepository

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager

  @BindValue @Mock lateinit var keyManager: KeyManager

  var messageSenderHelper = MessageSenderHelper()

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(KeyException::class, UnknownLaoException::class)
      override fun before() {
        hiltRule.inject()

        Mockito.`when`(laoRepo.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(BehaviorSubject.createDefault(LaoView(LAO)))
        Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenAnswer { LaoView(LAO) }

        val rollcallName = "rollcall#1"
        val rollCallId = generateCreateRollCallId(LAO.id, 1612204910, rollcallName)
        val popToken = Base64DataUtils.generatePoPToken()
        val attendees = HashSet<PublicKey>()
        attendees.add(popToken.publicKey)
        rollCallRepository.updateRollCall(
          LAO.id,
          RollCall(
            rollCallId,
            rollCallId,
            rollcallName,
            1612204910,
            1632204910,
            1632204900,
            EventState.CLOSED,
            LinkedHashSet(attendees),
            "bc",
            ""
          )
        )

        Mockito.`when`(networkManager.messageSender).thenReturn(messageSenderHelper.mockedSender)
        messageSenderHelper.setupMock()
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER_KEY.publicKey)
        Mockito.`when`(
            keyManager.getValidPoPToken(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
          )
          .thenReturn(popToken)
        Mockito.`when`(
            keyManager.getLongTermAuthToken(
              ArgumentMatchers.anyString(),
              ArgumentMatchers.anyString()
            )
          )
          .thenReturn(AuthToken(popToken))
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, PoPCHAHomeFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO.id).build(),
      LaoActivityPageObject.containerId(),
      PoPCHAHomeFragment::class.java
    ) {
      PoPCHAHomeFragment.newInstance()
    }

  @Test
  fun testPageHeaderText() {
    val expectedHeader =
      String.format("Welcome to the PoPCHA tab, you're currently in the LAO:\n%s", LAO.id)
    PoPCHAHomePageObject.getHeader()
      .check(ViewAssertions.matches(ViewMatchers.withText(expectedHeader)))
  }

  @Test
  fun testOpenScanner() {
    PoPCHAHomePageObject.getScanner().check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    PoPCHAHomePageObject.getScanner().perform(ViewActions.click())
    PoPCHAHomePageObject.getScannerFragment()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun testScanValidPoPCHAUrlSendMessage() {
    PoPCHAHomePageObject.getScanner().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_popcha_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(VALID_POPCHA_URL))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    Mockito.verify(messageSenderHelper.mockedSender)
      .publish(
        MockitoKotlinHelpers.any(),
        MockitoKotlinHelpers.eq(getLaoChannel(LAO.id).subChannel(PoPCHAViewModel.AUTHENTICATION)),
        MockitoKotlinHelpers.any()
      )
  }

  @Test
  fun testScanInvalidPoPCHAUrlFails() {

    PoPCHAHomePageObject.getScanner().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_popcha_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(INVALID_POPCHA_URL))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    Mockito.verifyNoInteractions(messageSenderHelper.mockedSender)
    UITestUtils.assertToastIsDisplayedWithText(R.string.invalid_qrcode_popcha_data)
  }

  @Test
  fun addingEmptyURLDoesNotAddAttendees() {
    PoPCHAHomePageObject.getScanner().perform(ViewActions.click())
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_popcha_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(EMPTY_POPCHA_URL))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    UITestUtils.assertToastIsDisplayedWithText(R.string.qrcode_scanning_manual_entry_error)
  }

  @Test
  fun testBackButtonBehaviour() {
    PoPCHAHomePageObject.getRootView().perform(ViewActions.pressBack())
    PoPCHAHomePageObject.getEventListFragment()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val LAO = Lao("laoName", SENDER_KEY.publicKey, 1612204910)
    private val VALID_POPCHA_URL =
      ("http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=" +
              "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=" +
              "http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=${LAO.id}" +
              "&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0ium" +
              "u_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1CO" +
              "HZsh1rElqimOTLAp3CbhbYJQ")
    private val INVALID_POPCHA_URL = "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=" +
            "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=" +
            "http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=" +
            "random_invalid_lao_id" +
            "&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0ium" +
            "u_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1CO" +
            "HZsh1rElqimOTLAp3CbhbYJQ"
    private val EMPTY_POPCHA_URL = ""
  }
}
