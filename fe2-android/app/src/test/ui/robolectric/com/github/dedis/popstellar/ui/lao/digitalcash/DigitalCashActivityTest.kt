package com.github.dedis.popstellar.ui.lao.digitalcash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptOutput
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction.Companion.computeSigOutputsPairTxOutHashAndIndex
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.model.objects.InputObject
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObjectBuilder
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.digitalcash.DigitalCashPageObject
import com.github.dedis.popstellar.testutils.pages.lao.digitalcash.HistoryPageObject
import com.github.dedis.popstellar.testutils.pages.lao.digitalcash.IssuePageObject
import com.github.dedis.popstellar.testutils.pages.lao.digitalcash.ReceivePageObject
import com.github.dedis.popstellar.testutils.pages.lao.digitalcash.SendPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.Collections
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DigitalCashActivityTest {
  @Inject lateinit var gson: Gson

  @BindValue @Mock lateinit var networkManager: GlobalNetworkManager

  @BindValue @Mock lateinit var laoRepo: LAORepository

  @BindValue @Mock lateinit var rollCallRepo: RollCallRepository

  @BindValue @Mock lateinit var digitalCashRepo: DigitalCashRepository

  @BindValue @Mock lateinit var messageSender: MessageSender

  @BindValue @Mock lateinit var keyManager: KeyManager

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      @Throws(KeyException::class, GeneralSecurityException::class, UnknownLaoException::class)
      override fun before() {
        hiltRule.inject()

        Mockito.`when`(rollCallRepo.getLastClosedRollCall(MockitoKotlinHelpers.any()))
          .thenReturn(ROLL_CALL)
        Mockito.`when`(laoRepo.getLaoView(MockitoKotlinHelpers.any())).thenReturn(LaoView(LAO))

        val builder = TransactionObjectBuilder()
        builder.setVersion(1)
        builder.setLockTime(0)
        builder.setChannel(fromString("/root/laoId/coin/myChannel"))
        val so = ScriptOutput("P2PKH", POP_TOKEN.publicKey.computeHash())
        val soo = ScriptOutputObject("P2PKH", POP_TOKEN.publicKey.computeHash())
        val oo = OutputObject(10, soo)
        val out = Output(10, so)
        val sig =
          POP_TOKEN.privateKey.sign(
            Base64URLData(
              computeSigOutputsPairTxOutHashAndIndex(
                  listOf(out),
                  Collections.singletonMap("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", 0)
                )
                .toByteArray(StandardCharsets.UTF_8)
            )
          )
        val sio = ScriptInputObject("P2PKH", POP_TOKEN.publicKey, sig)
        val io = InputObject("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", 0, sio)
        builder.setOutputs(listOf(oo))
        builder.setInputs(listOf(io))
        builder.setTransactionId("some id")
        val transaction = builder.build()

        val rcIdSet: MutableSet<RollCall> = HashSet()
        rcIdSet.add(ROLL_CALL)

        Mockito.`when`(
            digitalCashRepo.getTransactions(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(listOf(transaction))
        Mockito.`when`(
            digitalCashRepo.getTransactionsObservable(
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any()
            )
          )
          .thenReturn(BehaviorSubject.createDefault(listOf(transaction)))
        Mockito.`when`(rollCallRepo.getRollCallsObservableInLao(MockitoKotlinHelpers.any()))
          .thenReturn(BehaviorSubject.createDefault(rcIdSet))
        digitalCashRepo.updateTransactions(LAO.id, transaction)

        Mockito.`when`(laoRepo.getLaoObservable(ArgumentMatchers.anyString()))
          .thenReturn(BehaviorSubject.createDefault(LaoView(LAO)))
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(POP_TOKEN.publicKey)
        Mockito.`when`(
            keyManager.getValidPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .thenReturn(POP_TOKEN)
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
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, DigitalCashHomeFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      DigitalCashHomeFragment::class.java
    ) {
      DigitalCashHomeFragment()
    }

  @Test
  fun sendButtonGoesToSendThenToReceipt() {
    DigitalCashPageObject.sendButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(withId(SendPageObject.fragmentDigitalCashSendId()))
        )
      )
    SendPageObject.sendButtonToReceipt().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(withId(SendPageObject.fragmentDigitalCashSendId()))
        )
      )
  }

  @Test
  fun historyButtonGoesToHistory() {
    DigitalCashPageObject.historyButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(
            withId(HistoryPageObject.fragmentDigitalCashHistoryId())
          )
        )
      )
  }

  @Test
  fun historyElementsAreExpandable() {
    // Ensure the Digital Cash screen is displayed
    DigitalCashPageObject.historyButton().perform(ViewActions.click())

    // Click on the first transaction
    onView(withId(HistoryPageObject.transactionCardView()))
      .perform(ViewActions.click())

    // Check if the transaction details are displayed
    onView(withId(HistoryPageObject.transactionProvenanceTitle())
    )
      .check(matches(isDisplayed()))
    onView(withId(HistoryPageObject.transactionProvenanceValue())
    )
      .check(matches(isDisplayed()))
    onView(withId(HistoryPageObject.transactionIdValue())
    )
      .check(matches(isDisplayed()))
    onView(withId(HistoryPageObject.transactionIdTitle())
    )
      .check(matches(isDisplayed()))
  }

  @Test
  fun issueButtonGoesToIssue() {
    DigitalCashPageObject.issueButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(withId(IssuePageObject.fragmentDigitalCashIssueId()))
        )
      )
  }

  @Test
  fun issueButtonsWork(){
    DigitalCashPageObject.issueButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(withId(IssuePageObject.fragmentDigitalCashIssueId()))
        )
      )

    // open the spinner
    IssuePageObject.spinner().perform(ViewActions.click())
    //close the spinner
    IssuePageObject.spinner().perform(ViewActions.click())
    // select the radio button
    IssuePageObject.radioButtonAttendees().perform(ViewActions.click())
    // input amount
    IssuePageObject.issueAmount().perform(ViewActions.typeText("500"))
    // click issue button
    IssuePageObject.issueButton().perform(ViewActions.click())
  }

  @Test
  fun receiveButtonGoesToReceive() {
    DigitalCashPageObject.receiveButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(
            withId(ReceivePageObject.fragmentDigitalCashReceiveId())
          )
        )
      )
  }

  @Test
  fun historyButtonOnHistoryFragmentGoesBack() {
    DigitalCashPageObject.historyButton().perform(ViewActions.click())
    DigitalCashPageObject.historyButton().perform(ViewActions.click())
    LaoActivityPageObject.fragmentContainer()
      .check(
        matches(
          ViewMatchers.withChild(
            withId(DigitalCashPageObject.fragmentDigitalCashHomeId())
          )
        )
      )
  }

  companion object {
    private val POP_TOKEN = Base64DataUtils.generatePoPToken()
    private const val LAO_NAME = "LAO"
    private val LAO = Lao(LAO_NAME, POP_TOKEN.publicKey, 10223421)
    private val LAO_ID = LAO.id
    private const val RC_TITLE = "Roll-Call Title"
    private val ROLL_CALL =
      RollCall(
        "id",
        "id",
        RC_TITLE,
        0,
        1,
        2,
        EventState.CLOSED,
        mutableSetOf(POP_TOKEN.publicKey),
        "location",
        "desc"
      )
  }
}
