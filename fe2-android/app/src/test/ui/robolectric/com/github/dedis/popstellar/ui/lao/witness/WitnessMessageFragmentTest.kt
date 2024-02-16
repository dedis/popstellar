package com.github.dedis.popstellar.ui.lao.witness

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.witness.WitnessMessageFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import org.hamcrest.CoreMatchers
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
class WitnessMessageFragmentTest {
  private lateinit var witnessingViewModel: WitnessingViewModel
  private lateinit var witnessMessages: List<WitnessMessage>

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
      @Throws(UnknownLaoException::class, KeyException::class)
      override fun before() {
        hiltRule.inject()

        Mockito.`when`(laoRepo.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenAnswer { LaoView(LAO) }
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER)
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSenderHelper.mockedSender)
        messageSenderHelper.setupMock()
        Mockito.`when`(
            keyManager.getPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(POP_TOKEN)
        WITNESS_MESSAGE.title = TITLE
        WITNESS_MESSAGE.description = DESCRIPTION
        WITNESS_MESSAGE.addWitness(WITNESS1)
        WITNESS_MESSAGE.addWitness(WITNESS2)
        witnessMessages = listOf(WITNESS_MESSAGE)
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, WitnessMessageFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      WitnessMessageFragment::class.java
    ) {
      WitnessMessageFragment()
    }

  @Test
  fun testWitnessMessageListDisplaysMessageTitle() {
    witnessingViewModel = ViewModelProvider(laoActivity)[WitnessingViewModel::class.java]
    witnessingViewModel.setWitnessMessages(witnessMessages)

    WitnessMessageFragmentPageObject.witnessMessageList()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(TITLE))))
  }

  @Test
  fun testSignButtonState() {
    witnessingViewModel = ViewModelProvider(laoActivity)[WitnessingViewModel::class.java]
    witnessingViewModel.setWitnessMessages(witnessMessages)

    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .check(
        ViewAssertions.matches(
          ViewMatchers.hasDescendant(WitnessMessageFragmentPageObject.signMessageButtonMatcher())
        )
      )
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .onChildView(WitnessMessageFragmentPageObject.signMessageButtonMatcher())
      .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
  }

  @Test
  fun testWitnessMessageDescriptionDropdown() {
    witnessingViewModel = ViewModelProvider(laoActivity)[WitnessingViewModel::class.java]
    witnessingViewModel.setWitnessMessages(witnessMessages)

    // Check that the description title is displayed
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .check(
        ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(DESCRIPTION_TITLE)))
      )

    // Check that the message description is not displayed by default
    WitnessMessageFragmentPageObject.messageDescriptionText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )

    // Click on the arrow to expand the text
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .onChildView(WitnessMessageFragmentPageObject.messageDescriptionArrowMatcher())
      .perform(ViewActions.click())
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Check that the correct message description is displayed
    WitnessMessageFragmentPageObject.messageDescriptionText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .check(ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(DESCRIPTION))))
  }

  @Test
  fun testWitnessMessageSignaturesDropdown() {
    witnessingViewModel = ViewModelProvider(laoActivity)[WitnessingViewModel::class.java]
    witnessingViewModel.setWitnessMessages(witnessMessages)

    // Check that the signatures title is displayed
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .check(
        ViewAssertions.matches(ViewMatchers.hasDescendant(ViewMatchers.withText(SIGNATURES_TITLE)))
      )

    // Check that the signatures are not displayed by default
    WitnessMessageFragmentPageObject.witnessSignaturesText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )

    // Click on the arrow to expand the text
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .onChildView(WitnessMessageFragmentPageObject.messageSignaturesArrowMatcher())
      .perform(ViewActions.click())
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    // Check that the correct signatures are displayed
    WitnessMessageFragmentPageObject.witnessSignaturesText()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    Espresso.onData(CoreMatchers.anything())
      .inAdapterView(WitnessMessageFragmentPageObject.witnessMessageListMatcher())
      .atPosition(0)
      .check(
        ViewAssertions.matches(
          ViewMatchers.hasDescendant(ViewMatchers.withSubstring(WITNESS1.encoded))
        )
      )
      .check(
        ViewAssertions.matches(
          ViewMatchers.hasDescendant(ViewMatchers.withSubstring(WITNESS2.encoded))
        )
      )
  }

  private val laoActivity: LaoActivity
    get() {
      val ref = AtomicReference<LaoActivity>()
      activityScenarioRule.scenario.onActivity { newValue: LaoActivity -> ref.set(newValue) }
      return ref.get()
    }

  companion object {
    private const val LAO_NAME = "lao"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val LAO = Lao(LAO_NAME, SENDER, Instant.now().epochSecond)
    private val LAO_ID = LAO.id
    private val MESSAGE_ID1 = Base64DataUtils.generateMessageID()
    private val WITNESS_MESSAGE = WitnessMessage(MESSAGE_ID1)
    private const val TITLE = "Message title"
    private const val DESCRIPTION = "roll call name: test roll call"
    private const val DESCRIPTION_TITLE = "Description"
    private const val SIGNATURES_TITLE = "Signatures"
    private val WITNESS1 = Base64DataUtils.generatePublicKey()
    private val WITNESS2 = Base64DataUtils.generatePublicKeyOtherThan(WITNESS1)
    private val SIGNATURES_TEXT = "${WITNESS2.encoded}\n${WITNESS1.encoded}\n"
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
    private val POP_TOKEN = Base64DataUtils.generatePoPToken()
  }
}
