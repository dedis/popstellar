package com.github.dedis.popstellar.ui.lao.token

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.RollCall.Companion.closeRollCall
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.token.TokenListPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
import java.security.GeneralSecurityException
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TokenListFragmentTest {
  private val ROLL_CALL =
    RollCall(
      LAO.id,
      LAO.id,
      ROLL_CALL_TITLE,
      CREATION,
      ROLL_CALL_START,
      ROLL_CALL_END,
      EventState.CREATED,
      HashSet(),
      LOCATION,
      ROLL_CALL_DESC
    )

  @Inject lateinit var rollCallRepo: RollCallRepository

  @BindValue @Mock lateinit var repository: LAORepository

  @BindValue @Mock lateinit var keyManager: KeyManager

  @BindValue @Mock lateinit var wallet: Wallet

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(
        UnknownLaoException::class,
        GeneralSecurityException::class,
        KeyGenerationException::class,
        UninitializedWalletException::class
      )
      override fun before() {
        hiltRule.inject()
        Mockito.`when`(repository.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(laoSubject)
        Mockito.`when`(repository.getLaoView(MockitoKotlinHelpers.any())).thenAnswer {
          LaoView(LAO)
        }
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(USER)
        Mockito.`when`(wallet.exportSeed())
          .thenReturn(
            arrayOf(
              "jar",
              "together",
              "minor",
              "alley",
              "glow",
              "hybrid",
              "village",
              "creek",
              "meadow",
              "atom",
              "travel",
              "bracket"
            )
          )
        Mockito.`when`(
            wallet.generatePoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(USER_TOKEN)
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, TokenListFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      TokenListFragment::class.java
    ) {
      TokenListFragment.newInstance()
    }

  @Test
  fun noRollCallDisplaysExplanationMessage() {
    checkExplanationMessageIsDisplayed()
  }

  @Test
  fun noClosedRollCallDisplaysExplanationMessage() {
    setRollCalls(ROLL_CALL)
    checkExplanationMessageIsDisplayed()
  }

  @Test
  fun noRollCallAttendedDisplaysExplanationMessage() {
    val closedRollCallWithoutUser = closeRollCall(ROLL_CALL)
    setRollCalls(closedRollCallWithoutUser)
    checkExplanationMessageIsDisplayed()
  }

  @Test
  @Throws(NoRollCallException::class)
  fun havingAttendedClosedRollCallDisplayValidToken() {
    val closedRollCallWithUser =
      RollCallBuilder(ROLL_CALL)
        .setState(EventState.CLOSED)
        .setAttendees(setOf(USER_TOKEN.publicKey))
        .build()
    setRollCalls(closedRollCallWithUser)

    TokenListPageObject.emptyTokenText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    TokenListPageObject.invalidTokensRv()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    TokenListPageObject.validTokenCard()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
  }

  @Test
  @Throws(NoRollCallException::class)
  fun havingAttendedTwoClosedRollCallDisplayValidToken() {
    val closedRollCallWithUser1 =
      RollCallBuilder(ROLL_CALL)
        .setState(EventState.CLOSED)
        .setCreation(1000)
        .setStart(1000)
        .setEnd(1000)
        .setPersistentId("some ridiculous id")
        .setAttendees(setOf(USER_TOKEN.publicKey))
        .build()
    val closedRollCallWithUser2 =
      RollCallBuilder(ROLL_CALL).setState(EventState.CLOSED).setAttendees(emptySet()).build()
    setRollCalls(closedRollCallWithUser1, closedRollCallWithUser2)

    TokenListPageObject.emptyTokenText()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    TokenListPageObject.invalidTokensRv()
      .check(
        ViewAssertions.matches(
          ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
      )
    TokenListPageObject.validTokenCard()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  private fun setRollCalls(vararg rollCalls: RollCall) {
    for (rollCall in rollCalls) {
      rollCallRepo.updateRollCall(LAO_ID, rollCall)
    }
  }

  private fun checkExplanationMessageIsDisplayed() {
    TokenListPageObject.emptyTokenText()
      .check(ViewAssertions.matches(ViewMatchers.withText(R.string.empty_tokens_text)))
    TokenListPageObject.validTokenCard()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
    TokenListPageObject.invalidTokensRv()
      .check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE))
      )
  }

  companion object {
    private const val LAO_NAME = "lao"
    private val USER_KEY_PAIR = Base64DataUtils.generateKeyPair()
    private val USER = USER_KEY_PAIR.publicKey
    private val USER_TOKEN = Base64DataUtils.generatePoPToken()
    private val LAO = Lao(LAO_NAME, Base64DataUtils.generateKeyPair().publicKey, 10223421)
    private val LAO_ID = LAO.id
    private const val ROLL_CALL_TITLE = "RC title"
    private const val CREATION: Long = 10323411
    private const val ROLL_CALL_START: Long = 10323421
    private const val ROLL_CALL_END: Long = 10323431
    private const val ROLL_CALL_DESC = ""
    private const val LOCATION = "EPFL"
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
  }
}
