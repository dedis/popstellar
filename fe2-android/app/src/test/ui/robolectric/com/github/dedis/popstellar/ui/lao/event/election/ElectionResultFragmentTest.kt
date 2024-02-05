package com.github.dedis.popstellar.ui.lao.event.election

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.election.ElectionResultFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionResultFragment
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import java.util.Collections
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
class ElectionResultFragmentTest {
  @Inject lateinit var electionRepository: ElectionRepository

  @BindValue @Mock lateinit var laoRepository: LAORepository

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

        Mockito.`when`(laoRepository.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(BehaviorSubject.createDefault(LaoView(LAO)))
        Mockito.`when`(laoRepository.getLaoView(MockitoKotlinHelpers.any())).thenAnswer {
          LaoView(LAO)
        }
        electionRepository.updateElection(ELECTION)

        Mockito.`when`(keyManager.mainPublicKey).thenReturn(SENDER)
        Mockito.`when`(networkManager.messageSender).thenReturn(messageSender)
        Mockito.`when`(messageSender.subscribe(MockitoKotlinHelpers.any())).then {
          Completable.complete()
        }
        Mockito.`when`(
            messageSender.publish(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .then { Completable.complete() }
        Mockito.`when`(
            messageSender.publish(
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any()
            )
          )
          .then { Completable.complete() }
      }
    }

  @JvmField
  @Rule(order = 3)
  val fragmentRule: ActivityFragmentScenarioRule<LaoActivity, ElectionResultFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      ElectionResultFragment::class.java
    ) {
      ElectionResultFragment.newInstance(ELECTION_ID)
    }

  @Test
  fun laoTitleMatches() {
    ElectionResultFragmentPageObject.electionResultLaoTitle()
      .check(ViewAssertions.matches(ViewMatchers.withText(LAO_NAME)))
  }

  @Test
  fun electionTitleMatches() {
    ElectionResultFragmentPageObject.electionResultElectionTitle()
      .check(ViewAssertions.matches(ViewMatchers.withText(TITLE)))
  }

  @Test
  fun question1ElementsAreDisplayed() {
    Espresso.onView(ViewMatchers.withText(ELECTION_QUESTION_TEXT))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    Espresso.onView(ViewMatchers.withText(ELECTION_BALLOT1))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    Espresso.onView(ViewMatchers.withText(ELECTION_BALLOT2))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    Espresso.onView(ViewMatchers.withText(ELECTION_BALLOT3))
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
    private const val ELECTION_QUESTION_TEXT = "question"
    private const val ELECTION_BALLOT1 = "ballot option 1"
    private const val ELECTION_BALLOT2 = "ballot option 2"
    private const val ELECTION_BALLOT3 = "ballot option 3"
    private const val PLURALITY = "Plurality"
    private const val RESULT1 = 7
    private const val RESULT2 = 0
    private const val RESULT3 = 5
    private val ELECTION_ID = generateElectionSetupId(LAO_ID, CREATION, TITLE)
    private val QUESTION =
      ElectionQuestion(
        ELECTION_ID,
        Question(
          ELECTION_QUESTION_TEXT,
          PLURALITY,
          listOf(ELECTION_BALLOT1, ELECTION_BALLOT2, ELECTION_BALLOT3),
          false
        )
      )
    private val ELECTION =
      ElectionBuilder(LAO_ID, CREATION, TITLE)
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .setElectionQuestions(listOf(QUESTION))
        .setStart(START)
        .setEnd(END)
        .setState(EventState.CREATED)
        .setResults(
          buildResultsMap(
            QUESTION.id,
            QuestionResult(ELECTION_BALLOT1, RESULT1),
            QuestionResult(ELECTION_BALLOT2, RESULT2),
            QuestionResult(ELECTION_BALLOT3, RESULT3)
          )
        )
        .build()

    private fun buildResultsMap(
      id: String,
      vararg questionResults: QuestionResult
    ): Map<String, Set<QuestionResult>> {
      return Collections.singletonMap<String, Set<QuestionResult>>(
        id,
        HashSet(listOf(*questionResults))
      )
    }
  }
}
