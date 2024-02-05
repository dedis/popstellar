package com.github.dedis.popstellar.ui.lao.event.consensus

import androidx.test.annotation.UiThreadTest
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.loadSchema
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.ElectInstance.Companion.generateConsensusId
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.event.consensus.ElectionStartPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.MessageHandler
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager
import com.google.crypto.tink.signature.PublicKeySignWrapper
import com.google.gson.Gson
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.hamcrest.core.AllOf
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.internal.util.collections.Sets
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ElectionStartFragmentTest {
  private lateinit var consensusChannel: Channel
  private lateinit var mainKeyPair: KeyPair
  private lateinit var node3KeyPair: KeyPair
  private lateinit var publicKey: String
  private lateinit var node2: String
  private lateinit var node3: String

  private var ownPos = 0
  private var node2Pos = 0
  private var node3Pos = 0

  @Inject lateinit var keyManager: KeyManager

  @Inject lateinit var messageHandler: MessageHandler

  @Inject lateinit var gson: Gson

  @Inject lateinit var electionRepo: ElectionRepository

  @Inject lateinit var consensusRepo: ConsensusRepository

  @Inject lateinit var laoRepo: LAORepository

  @BindValue @Mock lateinit var globalNetworkManager: GlobalNetworkManager

  @Mock lateinit var messageSender: MessageSender

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: TestRule =
    object : ExternalResource() {
      override fun before() {
        // Injection with hilt
        hiltRule.inject()

        // Preload the data schema before the test run
        loadSchema(JsonUtils.DATA_SCHEMA)
        val node2KeyPair: KeyPair
        try {
          Ed25519PrivateKeyManager.registerPair(true)
          PublicKeySignWrapper.register()
          val keysetHandle2 = KeysetHandle.generateNew(KeyTemplates.get("ED25519_RAW"))
          val keysetHandle3 = KeysetHandle.generateNew(KeyTemplates.get("ED25519_RAW"))
          mainKeyPair = keyManager.mainKeyPair
          node2KeyPair = keyManager.getKeyPair(keysetHandle2)
          node3KeyPair = keyManager.getKeyPair(keysetHandle3)
          publicKey = mainKeyPair.publicKey.encoded
          node2 = node2KeyPair.publicKey.encoded
          node3 = node3KeyPair.publicKey.encoded
        } catch (e: Exception) {
          throw RuntimeException(e)
        }

        val lao = Lao(LAO_ID)
        lao.organizer = mainKeyPair.publicKey
        consensusRepo.setOrganizer(lao.id, mainKeyPair.publicKey)
        consensusRepo.initKeyToNode(
          lao.id,
          Sets.newSet(node2KeyPair.publicKey, node3KeyPair.publicKey)
        )
        laoRepo.updateLao(lao)
        consensusChannel = lao.channel.subChannel("consensus")
        consensusRepo.updateNodesByChannel(lao.channel)
        val nodes = consensusRepo.getNodes(lao.id)
        for (i in nodes.indices) {
          when (nodes[i].publicKey.encoded) {
            publicKey -> {
              ownPos = i
            }
            node2 -> {
              node2Pos = i
            }
            else -> {
              node3Pos = i
            }
          }
        }
        electionRepo.updateElection(ELECTION)

        Mockito.`when`(globalNetworkManager.messageSender).thenReturn(messageSender)
        Mockito.`when`(
            messageSender.publish(
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any(),
              MockitoKotlinHelpers.any()
            )
          )
          .then { Completable.complete() }
        Mockito.`when`(
            messageSender.publish(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
          )
          .then { Completable.complete() }
        Mockito.`when`(messageSender.subscribe(MockitoKotlinHelpers.any())).then {
          Completable.complete()
        }
      }
    }

  @JvmField
  @Rule(order = 3)
  val fragmentRule: ActivityFragmentScenarioRule<LaoActivity, ElectionStartFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
      LaoActivityPageObject.containerId(),
      ElectionStartFragment::class.java
    ) {
      ElectionStartFragment.newInstance(ELECTION.id)
    }

  @Test
  @Throws(
    DataHandlingException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun displayWithUpdatesIsCorrect() {
    setElectionStart(PAST_TIME)

    // Election start time has passed, should display that it's ready and start button enabled
    displayAssertions(STATUS_READY, START_START, true)
    val grid = ElectionStartPageObject.nodesGrid()
    nodeAssertions(grid, ownPos, "Waiting\n$publicKey", false)
    nodeAssertions(grid, node2Pos, "Waiting\n$node2", false)
    nodeAssertions(grid, node3Pos, "Waiting\n$node3", false)

    // Nodes 3 try to start
    val elect3Msg = createMsg(node3KeyPair, elect)
    messageHandler.handleMessage(messageSender, consensusChannel, elect3Msg)
    nodeAssertions(grid, node3Pos, "Approve Start by\n$node3", true)

    // We try to start (it should disable the start button)
    val elect1Msg = createMsg(mainKeyPair, elect)
    messageHandler.handleMessage(messageSender, consensusChannel, elect1Msg)
    displayAssertions(STATUS_READY, START_START, false)
    nodeAssertions(grid, ownPos, "Approve Start by\n$publicKey", true)

    // We accepted node 3 (it should disable button for node3)
    val electAccept3 = ConsensusElectAccept(INSTANCE_ID, elect3Msg.messageId, true)
    val accept3Msg = createMsg(mainKeyPair, electAccept3)
    messageHandler.handleMessage(messageSender, consensusChannel, accept3Msg)
    nodeAssertions(grid, node3Pos, "Approve Start by\n$node3", false)

    // Receive a learn message => node3 was accepted and has started the election
    val learn3 = ConsensusLearn(INSTANCE_ID, elect3Msg.messageId, PAST_TIME, true, emptyList())
    val learn3Msg = createMsg(node3KeyPair, learn3)
    messageHandler.handleMessage(messageSender, consensusChannel, learn3Msg)
    displayAssertions(STATUS_STARTED, START_STARTED, false)
    nodeAssertions(grid, node3Pos, "Started by\n$node3", false)
  }

  @Test
  fun startDisabledOnStartupIfInFutureTest() {
    setElectionStart(FUTURE_TIME)
    displayAssertions(STATUS_WAITING, START_SCHEDULED, false)
  }

  @Test
  fun startEnabledOnStartupIfStartTimeInPastTest() {
    setElectionStart(PAST_TIME)
    displayAssertions(STATUS_READY, START_START, true)
  }

  @Test
  @UiThreadTest
  fun updateTest() {
    setElectionStart(FUTURE_TIME)
    // Election start time has not passed yet, should display that it's waiting
    displayAssertions(STATUS_WAITING, START_SCHEDULED, false)

    // Update election start time
    electionRepo.updateElection(ELECTION.builder().setStart(PAST_TIME).build())

    // Election start time has passed, should display that it's ready and start button enabled
    displayAssertions(STATUS_READY, START_START, true)
  }

  @Test
  fun startButtonSendElectMessageTest() {
    setElectionStart(PAST_TIME)

    val minCreation = Instant.now().epochSecond
    ElectionStartPageObject.electionStartButton().perform(ViewActions.click())
    val captor = MockitoKotlinHelpers.argumentCaptor<MessageGeneral>()

    Mockito.verify(messageSender)
      .publish(MockitoKotlinHelpers.eq(consensusChannel), MockitoKotlinHelpers.capture(captor))

    val msgGeneral = captor.value
    val maxCreation = Instant.now().epochSecond
    Assert.assertEquals(mainKeyPair.publicKey, msgGeneral.sender)

    val elect = msgGeneral.data as ConsensusElect
    Assert.assertEquals(KEY, elect.key)
    Assert.assertEquals(INSTANCE_ID, elect.instanceId)
    Assert.assertEquals("started", elect.value)
    Assert.assertTrue(elect.creation in minCreation..maxCreation)
  }

  @Test
  @Throws(
    DataHandlingException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun acceptButtonSendElectAcceptMessageTest() {
    setElectionStart(PAST_TIME)

    // Nodes 3 try to start
    val elect3Msg = createMsg(node3KeyPair, elect)
    messageHandler.handleMessage(messageSender, consensusChannel, elect3Msg)

    // We try to accept node3
    ElectionStartPageObject.nodesGrid().atPosition(node3Pos).perform(ViewActions.click())
    val captor = MockitoKotlinHelpers.argumentCaptor<ConsensusElectAccept>()
    Mockito.verify(messageSender)
      .publish(
        MockitoKotlinHelpers.eq(mainKeyPair),
        MockitoKotlinHelpers.eq(consensusChannel),
        MockitoKotlinHelpers.capture(captor)
      )
    val electAccept = captor.value
    val expectedElectAccept = ConsensusElectAccept(INSTANCE_ID, elect3Msg.messageId, true)
    Assert.assertEquals(expectedElectAccept, electAccept)
  }

  @Test
  @Throws(
    DataHandlingException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun failureTest() {
    setElectionStart(PAST_TIME)

    // Nodes 3 try to start and failed
    val elect3Msg = createMsg(node3KeyPair, elect)
    messageHandler.handleMessage(messageSender, consensusChannel, elect3Msg)
    val failure3 = ConsensusFailure(INSTANCE_ID, elect3Msg.messageId, PAST_TIME)
    val failure3Msg = createMsg(node3KeyPair, failure3)
    messageHandler.handleMessage(messageSender, consensusChannel, failure3Msg)
    nodeAssertions(ElectionStartPageObject.nodesGrid(), node3Pos, "Start Failed\n$node3", false)

    // We try to start and failed
    val elect1Msg = createMsg(mainKeyPair, elect)
    messageHandler.handleMessage(messageSender, consensusChannel, elect1Msg)
    val failure1 = ConsensusFailure(INSTANCE_ID, elect1Msg.messageId, PAST_TIME)
    val failure1Msg = createMsg(mainKeyPair, failure1)
    messageHandler.handleMessage(messageSender, consensusChannel, failure1Msg)

    InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    displayAssertions(STATUS_READY, START_START, true)
    nodeAssertions(ElectionStartPageObject.nodesGrid(), node3Pos, "Start Failed\n$node3", false)
    nodeAssertions(ElectionStartPageObject.nodesGrid(), node2Pos, "Waiting\n$node2", false)
    nodeAssertions(ElectionStartPageObject.nodesGrid(), ownPos, "Start Failed\n$publicKey", false)
  }

  private fun displayAssertions(expectedStatus: String, expectedStart: String, enabled: Boolean) {
    val expectedTitle = "Election \"$ELECTION_NAME\""
    ElectionStartPageObject.electionTitle()
      .check(ViewAssertions.matches(ViewMatchers.withText(expectedTitle)))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    ElectionStartPageObject.electionStatus()
      .check(ViewAssertions.matches(ViewMatchers.withText(expectedStatus)))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    ElectionStartPageObject.electionStartButton()
      .check(ViewAssertions.matches(ViewMatchers.withText(expectedStart)))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
      .check(
        ViewAssertions.matches(
          if (enabled) ViewMatchers.isEnabled() else ViewMatchers.isNotEnabled()
        )
      )
  }

  private fun nodeAssertions(
    grid: DataInteraction,
    position: Int,
    expectedText: String,
    enabled: Boolean
  ) {
    grid
      .atPosition(position)
      .check(
        ViewAssertions.matches(
          AllOf.allOf(
            ViewMatchers.isDisplayed(),
            ViewMatchers.withText(expectedText),
            if (enabled) ViewMatchers.isEnabled() else ViewMatchers.isNotEnabled()
          )
        )
      )
  }

  private fun setElectionStart(electionStart: Long) {
    electionRepo.updateElection(ELECTION.builder().setStart(electionStart).build())
  }

  private fun createMsg(nodeKey: KeyPair, data: Data): MessageGeneral {
    return MessageGeneral(nodeKey, data, gson)
  }

  companion object {
    private val DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").withZone(ZoneId.systemDefault())
    private const val LAO_ID = "laoId"
    private const val ELECTION_NAME = "My Election !"
    private const val PAST_TIME: Long = 946684800
    private const val FUTURE_TIME: Long = 2145916800
    private val ELECTION =
      ElectionBuilder(LAO_ID, PAST_TIME, ELECTION_NAME)
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .setState(EventState.CREATED)
        .build()
    private val KEY = ConsensusKey("election", ELECTION.id, "state")
    private val INSTANCE_ID = generateConsensusId(KEY.type, KEY.id, KEY.property)
    private val elect = ConsensusElect(PAST_TIME, KEY.id, KEY.type, KEY.property, "started")
    private const val STATUS_WAITING = "Waiting scheduled time"
    private const val STATUS_READY = "Ready to start"
    private const val STATUS_STARTED = "Started"
    private val DATE_FUTURE = DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(FUTURE_TIME))
    private val DATE_PAST = DATE_TIME_FORMATTER.format(Instant.ofEpochSecond(PAST_TIME))
    private val START_SCHEDULED =
      """
           Election scheduled to start at
           $DATE_FUTURE
           """
        .trimIndent()
    private const val START_START = "Start Election"
    private val START_STARTED =
      """
           Election started successfully at
           $DATE_PAST
           """
        .trimIndent()
  }
}
