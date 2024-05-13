package com.github.dedis.popstellar.ui.lao.socialmedia

import android.content.res.Configuration
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.UITestUtils
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.socialmedia.SocialMediaHomePageObject
import com.github.dedis.popstellar.testutils.pages.lao.socialmedia.SocialMediaSendPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
class SocialMediaHomeFragmentTest {

    private val ROLL_CALL =
            RollCall(
                    LAO_ID,
                    LAO_ID,
                    "",
                    CREATION_TIME,
                    CREATION_TIME,
                    CREATION_TIME,
                    EventState.CLOSED,
                    HashSet(),
                    "",
                    "",
            )

    @Inject
    lateinit var laoRepository: LAORepository

    @Inject
    lateinit var rollCallRepository: RollCallRepository

    @BindValue
    @Mock
    lateinit var keyManager: KeyManager

    @JvmField
    @Rule
    var rule = InstantTaskExecutorRule()

    @JvmField
    @Rule(order = 0)
    val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

    @JvmField
    @Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @JvmField
    @Rule(order = 2)
    val setupRule: ExternalResource =
            object : ExternalResource() {
                override fun before() {
                    hiltRule.inject()

                    laoRepository.updateLao(LAO)
                    rollCallRepository.updateRollCall(LAO_ID, ROLL_CALL)
                    Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER_KEY_1.publicKey)
                    Mockito.`when`(
                            keyManager.getValidPoPToken(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
                    )
                            .thenReturn(SENDER_KEY_1 as PoPToken)
                }
            }

    @JvmField
    @Rule(order = 3)
    var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, SocialMediaHomeFragment> =
            ActivityFragmentScenarioRule.launchIn(
                    LaoActivity::class.java,
                    BundleBuilder().putString(Constants.LAO_ID_EXTRA, LAO_ID).build(),
                    LaoActivityPageObject.containerId(),
                    SocialMediaHomeFragment::class.java
            ) {
                SocialMediaHomeFragment()
            }

    @Test
    fun testBackButtonBehaviour() {
        SocialMediaHomePageObject.getRootView().perform(ViewActions.pressBack())
        // Check current fragment displayed is event list
        SocialMediaHomePageObject.getEventListFragment()
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun handleRotationTest() {
        activityScenarioRule.scenario.onActivity { activity: LaoActivity ->
            val before = activity.supportFragmentManager.findFragmentById(R.id.fragment_container_lao)
            val config = Configuration(activity.resources.configuration)
            config.orientation = Configuration.ORIENTATION_LANDSCAPE
            activity.onConfigurationChanged(config)
            val after = activity.supportFragmentManager.findFragmentById(R.id.fragment_container_lao)

            Assert.assertEquals(after, before)
            Assert.assertTrue(after is SocialMediaHomeFragment)
        }
    }

    @Test
    fun testPoPTokenToastError() {
        Mockito.`when`(
                keyManager.getValidPoPToken(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
        ).thenThrow(NoRollCallException(LAO.id))

        activityScenarioRule.scenario.onActivity {
            // Check on launch of the activity this is not displayed
            UITestUtils.assertLatestToastContent(false, R.string.error_retrieve_own_token, "")

            val addChirpButton = SocialMediaHomePageObject.getAddChirpButton()
            Assert.assertNotNull(addChirpButton)
            addChirpButton.perform(ViewActions.click())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            val chirpText = SocialMediaSendPageObject.entryBoxChirpText()
            chirpText.perform(UITestUtils.forceTypeText("test"))
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            val sendButton = SocialMediaSendPageObject.sendChirpButton()
            sendButton.perform(ViewActions.click())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            UITestUtils.assertLatestToastContent(true, R.string.error_retrieve_own_token, "")
        }
    }

    companion object {
        private const val CREATION_TIME: Long = 1631280815
        private const val LAO_NAME = "laoName"
        private val SENDER_KEY_1: KeyPair = Base64DataUtils.generatePoPToken()
        private val SENDER_KEY_2: KeyPair = Base64DataUtils.generatePoPToken()
        private val SENDER_1 = SENDER_KEY_1.publicKey
        private val LAO_ID = generateLaoId(SENDER_1, CREATION_TIME, LAO_NAME)
        private val LAO = Lao(LAO_ID)
    }
}
