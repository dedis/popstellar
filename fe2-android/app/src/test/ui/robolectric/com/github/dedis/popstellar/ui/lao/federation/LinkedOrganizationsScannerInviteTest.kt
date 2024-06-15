package com.github.dedis.popstellar.ui.lao.federation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.PerformException
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
import com.github.dedis.popstellar.testutils.UITestUtils.forceTypeText
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.scanning.QrScanningPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LinkedOrganizationsScannerInviteTest {
    @BindValue @Mock lateinit var repository: LAORepository

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
                newInstance(ScanningAction.FEDERATION_INVITE)
            }

    @Test
    fun addButtonIsDisplayed() {
        QrScanningPageObject.openManualButton()
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun addingManualLaoInfoWorks() {
        QrScanningPageObject.openManualButton().perform(ViewActions.click())

        val input1 = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_organization_lao_id_hint)
        Assert.assertNotNull(input1)
        input1.perform(forceTypeText(LAO_ID))

        val input2 = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_organization_server_url_hint)
        Assert.assertNotNull(input2)
        input2.perform(forceTypeText(ADDRESS))

        val input3 = QrScanningPageObject.manualInputWithHintRes(R.string.manual_add_organization_organizer_public_key_hint)
        Assert.assertNotNull(input3)
        input3.perform(forceTypeText(SENDER.encoded))

        // Throws a NullPointerException when trying to open a new fragment as the ViewModel is null in this test
        assertThrows(NullPointerException::class.java) {
            try {
                QrScanningPageObject.manualAddConfirm().perform(ViewActions.click())
            } catch (e: PerformException) {
                throw e.cause ?: e
            }
        }
    }

    companion object {
        private const val LAO_NAME = "lao"
        private val SENDER_KEY = Base64DataUtils.generateKeyPair()
        private val SENDER = SENDER_KEY.publicKey
        private val LAO = Lao(LAO_NAME, SENDER, 10223421)
        private val LAO_ID = LAO.id
        private const val ADDRESS = "localhost:9100"
        private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
    }
}
