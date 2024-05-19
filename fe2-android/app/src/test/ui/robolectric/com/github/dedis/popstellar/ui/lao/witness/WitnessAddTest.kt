package com.github.dedis.popstellar.ui.lao.witness

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.UITestUtils.assertToastIsDisplayedWithText
import com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.github.dedis.popstellar.utility.error.UnknownLaoException
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
import java.time.Instant

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WitnessAddTest {
  @BindValue @Mock lateinit var repository: LAORepository

  @BindValue @Mock lateinit var keyManager: KeyManager

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

        Mockito.`when`(repository.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(repository.getLaoView(MockitoKotlinHelpers.any())).thenReturn(LaoView(LAO))
        Mockito.`when`(keyManager.mainKeyPair).thenReturn(SENDER_KEY)
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER_KEY.publicKey)
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, QrScannerFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      QrScannerFragment::class.java
    ) {
      newInstance(ScanningAction.ADD_WITNESS)
    }

  @Test
  fun addButtonIsDisplayed() {
    QrScanningPageObject.openManualButton()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun addingValidManualEntry() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(VALID_WITNESS_MANUAL_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    assertToastIsDisplayedWithText(R.string.witness_scan_success)
  }

  @Test
  fun addingInvalidJsonFormatDoesNotAddAttendees() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(INVALID_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    assertToastIsDisplayedWithText(R.string.qr_code_not_main_pk)
  }

  @Test
  fun addingEmptyKeyDoesNotAddAttendees() {
    QrScanningPageObject.openManualButton().perform(ViewActions.click())
    val input = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_witness_hint)
    Assert.assertNotNull(input)
    input.perform(forceTypeText(EMPTY_KEY_FORMAT_INPUT))
    QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())

    assertToastIsDisplayedWithText(R.string.qrcode_scanning_manual_entry_error)
  }

  companion object {
    private const val LAO_NAME = "lao"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val CREATION = Instant.now().epochSecond
    private val LAO = Lao(LAO_NAME, SENDER, CREATION)
    private val LAO_ID = LAO.id
    private val POP_TOKEN = Base64DataUtils.generatePoPToken().publicKey.encoded
    private val INVALID_INPUT = "invalid for sure"
    private val VALID_WITNESS_MANUAL_INPUT = POP_TOKEN
    private val EMPTY_KEY_FORMAT_INPUT = ""
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
  }
}
