package com.github.dedis.popstellar.ui.lao.event.election

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MessageSenderHelper
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.election.CastVoteFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.event.election.fragments.CastVoteFragment
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.subjects.BehaviorSubject
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
class CastVoteSecretBallotFragmentTest {
  @Inject lateinit var electionRepo: ElectionRepository

  @Inject lateinit var rollCallRepo: RollCallRepository

  @BindValue @Mock lateinit var laoRepo: LAORepository

  @BindValue @Mock lateinit var keyManager: KeyManager

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager
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
          .thenReturn(laoSubject)
        Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenAnswer { LaoView(LAO) }
        rollCallRepo.updateRollCall(LAO_ID, ROLL_CALL)
        electionRepo.updateElection(ELECTION)

        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER)
        Mockito.`when`(
            keyManager.getValidPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(Base64DataUtils.generatePoPToken())
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSenderHelper.mockedSender)

        messageSenderHelper.setupMock()
      }
    }

  @JvmField
  @Rule(order = 3)
  val fragmentRule: ActivityFragmentScenarioRule<LaoActivity, CastVoteFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      CastVoteFragment::class.java
    ) {
      CastVoteFragment.newInstance(ELECTION_ID)
    }

  @Test
  fun castEncryptedVoteTest() {
    Espresso.onView(ViewMatchers.withText(ELECTION_BALLOT_TEXT11)).perform(ViewActions.click())
    CastVoteFragmentPageObject.castVotePager().perform(ViewActions.swipeLeft())
    Espresso.onView(ViewMatchers.withText(ELECTION_BALLOT_TEXT22)).perform(ViewActions.click())
    CastVoteFragmentPageObject.castVoteButton().perform(ViewActions.click())
    // Wait for the operations performed above to complete
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    CastVoteFragmentPageObject.encryptedVoteText()
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
  }

  companion object {
    private const val LAO_NAME = "LAO"
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val LAO = Lao(LAO_NAME, SENDER, 10223421)
    private val LAO_ID = LAO.id
    private const val TITLE = "Election name"
    private const val CREATION: Long = 10323411
    private const val START: Long = 10323421
    private const val END: Long = 10323431
    private const val ELECTION_QUESTION_TEXT1 = "question 1"
    private const val ELECTION_QUESTION_TEXT2 = "question 2"
    private const val ELECTION_BALLOT_TEXT11 = "ballot option 1"
    private const val ELECTION_BALLOT_TEXT12 = "ballot option 2"
    private const val ELECTION_BALLOT_TEXT13 = "ballot option 3"
    private const val ELECTION_BALLOT_TEXT21 = "random 1"
    private const val ELECTION_BALLOT_TEXT22 = "random 2"
    private const val PLURALITY = "Plurality"
    private val laoSubject = BehaviorSubject.createDefault(LaoView(LAO))
    private val ROLL_CALL =
      RollCall("id", "id", "rc", 0L, 1L, 2L, EventState.CLOSED, HashSet(), "nowhere", "none")
    private val ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, TITLE)
    private val ELECTION_QUESTION_1 =
      ElectionQuestion(
        ELECTION_ID,
        Question(
          ELECTION_QUESTION_TEXT1,
          PLURALITY,
          listOf(ELECTION_BALLOT_TEXT11, ELECTION_BALLOT_TEXT12, ELECTION_BALLOT_TEXT13),
          false
        )
      )
    private val ELECTION_QUESTION_2 =
      ElectionQuestion(
        ELECTION_ID,
        Question(
          ELECTION_QUESTION_TEXT2,
          PLURALITY,
          listOf(ELECTION_BALLOT_TEXT21, ELECTION_BALLOT_TEXT22),
          false
        )
      )
    private val ELECTION =
      ElectionBuilder(LAO_ID, CREATION, TITLE)
        .setElectionVersion(ElectionVersion.SECRET_BALLOT)
        .setElectionQuestions(listOf(ELECTION_QUESTION_1, ELECTION_QUESTION_2))
        .setStart(START)
        .setEnd(END)
        .setState(EventState.CREATED)
        .build()
  }
}
