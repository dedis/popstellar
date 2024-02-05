package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionKey
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionOpen
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.method.message.data.election.EncryptedVote
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import com.github.dedis.popstellar.model.network.method.message.data.election.Vote
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Election.Companion.generateElectionSetupId
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.elGamal.ElectionKeyPair.Companion.generateKeyPair
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidChannelException
import com.github.dedis.popstellar.utility.error.InvalidStateException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.data.ElectionHandler.Companion.electionSetupWitnessMessage
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.stream.Collectors
import java.util.stream.Stream
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class ElectionHandlerTest {
  private lateinit var electionRepo: ElectionRepository
  private lateinit var witnessingRepository: WitnessingRepository
  private lateinit var messageHandler: MessageHandler
  private lateinit var messageRepo: MessageRepository
  private lateinit var gson: Gson

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var rollCallRepo: RollCallRepository

  @Mock lateinit var meetingRepo: MeetingRepository

  @Mock lateinit var digitalCashRepo: DigitalCashRepository

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var electionDao: ElectionDao

  @Mock lateinit var witnessingDao: WitnessingDao

  @Mock lateinit var witnessDao: WitnessDao

  @Mock lateinit var pendingDao: PendingDao

  @Mock lateinit var messageSender: MessageSender

  @Mock lateinit var keyManager: KeyManager

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @Before
  @Throws(GeneralSecurityException::class, IOException::class)
  fun setup() {
    MockitoAnnotations.openMocks(this)

    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.lenient().`when`(keyManager.mainKeyPair).thenReturn(SENDER_KEY)
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(SENDER)
    Mockito.`when`(messageSender.subscribe(MockitoKotlinHelpers.any())).then {
      Completable.complete()
    }

    Mockito.`when`(appDatabase.laoDao()).thenReturn(laoDao)
    Mockito.`when`(laoDao.allLaos).thenReturn(Single.just(ArrayList()))
    Mockito.`when`(laoDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())

    Mockito.`when`(appDatabase.messageDao()).thenReturn(messageDao)
    Mockito.`when`(messageDao.takeFirstNMessages(ArgumentMatchers.anyInt()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(messageDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(messageDao.getMessageById(MockitoKotlinHelpers.any())).thenReturn(null)

    Mockito.`when`(appDatabase.electionDao()).thenReturn(electionDao)
    Mockito.`when`(electionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(electionDao.getElectionsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))

    Mockito.`when`(appDatabase.witnessDao()).thenReturn(witnessDao)
    Mockito.`when`(witnessDao.getWitnessesByLao(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(witnessDao.insertAll(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(witnessDao.isWitness(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any()))
      .thenReturn(0)

    Mockito.`when`(appDatabase.witnessingDao()).thenReturn(witnessingDao)
    Mockito.`when`(witnessingDao.getWitnessMessagesByLao(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(witnessingDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(
        witnessingDao.deleteMessagesByIds(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
      )
      .thenReturn(Completable.complete())

    Mockito.`when`(appDatabase.pendingDao()).thenReturn(pendingDao)
    Mockito.`when`(pendingDao.getPendingObjectsFromLao(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(pendingDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(pendingDao.removePendingObject(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())

    val laoRepo = LAORepository(appDatabase, application)
    electionRepo = ElectionRepository(appDatabase, application)
    witnessingRepository =
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepo,
        electionRepo,
        meetingRepo,
        digitalCashRepo
      )
    messageRepo = MessageRepository(appDatabase, application)

    val dataRegistry =
      buildRegistry(laoRepo, electionRepo, witnessingRepository, keyManager, messageRepo)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)
    laoRepo.updateLao(LAO)

    // Add the CreateLao message to the LAORepository
    val createLaoMessage = MessageGeneral(SENDER_KEY, CREATE_LAO, gson)
    messageRepo.addMessage(createLaoMessage, isContentNeeded = true, toPersist = true)
  }

  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleElectionSetup(election: Election, channel: Channel): MessageID {
    val questions =
      election.electionQuestions
        .stream()
        .map { elecQuestion: ElectionQuestion ->
          Question(
            elecQuestion.question,
            elecQuestion.votingMethod,
            elecQuestion.ballotOptions,
            elecQuestion.writeIn
          )
        }
        .collect(Collectors.toList())

    // Create the setup Election message
    val electionSetupOpenBallot =
      ElectionSetup(
        election.name,
        election.creation,
        election.startTimestamp,
        election.endTimestamp,
        LAO.id,
        election.electionVersion,
        questions
      )
    val message = MessageGeneral(SENDER_KEY, electionSetupOpenBallot, gson)

    messageHandler.handleMessage(messageSender, channel, message)
    return message.messageId
  }

  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleElectionKey(election: Election, key: String) {
    // Create the election key message
    val electionKey = ElectionKey(election.id, key)
    val message = MessageGeneral(SENDER_KEY, electionKey, gson)

    messageHandler.handleMessage(messageSender, election.channel, message)
  }

  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleElectionOpen(election: Election) {
    val electionOpen = ElectionOpen(LAO.id, election.id, OPENED_AT)
    val message = MessageGeneral(SENDER_KEY, electionOpen, gson)

    messageHandler.handleMessage(messageSender, election.channel, message)
  }

  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleCastVote(vote: Vote, senderKey: KeyPair, creation: Long): MessageID {
    val castVote = CastVote(listOf(vote), OPEN_BALLOT_ELECTION.id, CREATE_LAO.id, creation)
    val message = MessageGeneral(senderKey, castVote, gson)

    messageHandler.handleMessage(messageSender, OPEN_BALLOT_ELECTION.channel, message)
    return message.messageId
  }

  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleElectionEnd() {
    // Retrieve current election to use the correct vote hash
    val current = electionRepo.getElection(LAO.id, ELECTION_ID)
    val endElection = ElectionEnd(LAO.id, ELECTION_ID, current.computeRegisteredVotesHash())

    val message = MessageGeneral(SENDER_KEY, endElection, gson)
    messageHandler.handleMessage(messageSender, OPEN_BALLOT_ELECTION.channel, message)
  }

  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleElectionResults(results: Set<QuestionResult>, electionChannel: Channel) {
    val electionResultQuestion = ElectionResultQuestion(QUESTION.id, results)
    val electionResult = ElectionResult(listOf(electionResultQuestion))
    val message = MessageGeneral(SENDER_KEY, electionResult, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, electionChannel, message)
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
  fun testHandleElectionSetup() {
    val messageID = handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)

    // Check the Election is present and has correct values
    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)

    Assert.assertEquals(EventState.CREATED, election.state)
    Assert.assertEquals(OPEN_BALLOT_ELECTION.id, election.id)
    Assert.assertEquals(OPEN_BALLOT_ELECTION.electionQuestions, election.electionQuestions)
    Assert.assertEquals(OPEN_BALLOT_ELECTION.electionVersion, election.electionVersion)
    Assert.assertEquals(OPEN_BALLOT_ELECTION.startTimestamp, election.startTimestamp)
    Assert.assertEquals(OPEN_BALLOT_ELECTION.creation, election.creation)
    Assert.assertEquals(OPEN_BALLOT_ELECTION.endTimestamp, election.endTimestamp)

    // Check the WitnessMessage has been created
    val witnessMessage = witnessingRepository.getWitnessMessage(LAO_ID, messageID)
    Assert.assertTrue(witnessMessage.isPresent)

    // Check the Witness message contains the expected title and description
    val expectedMessage = electionSetupWitnessMessage(messageID, election)
    Assert.assertEquals(expectedMessage.title, witnessMessage.get().title)
    Assert.assertEquals(expectedMessage.description, witnessMessage.get().description)
  }

  @Test
  fun testHandleElectionSetupInvalidChannel() {
    // Check that handling the message fails with an invalid channel (channel that is not a lao
    // sub-channel)
    Assert.assertThrows(InvalidChannelException::class.java) {
      handleElectionSetup(OPEN_BALLOT_ELECTION, Channel.ROOT)
    }
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
  fun testElectionKey() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)

    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(ELECTION_KEY, election.electionKey)
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
  fun testHandleElectionResult() {
    val results = setOf(QuestionResult(OPTION_1, 1))

    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)
    handleCastVote(PlainVote(QUESTION.id, 0, false, null, ELECTION_ID), SENDER_KEY, OPENED_AT)
    handleElectionEnd()
    handleElectionResults(results, OPEN_BALLOT_ELECTION.channel)

    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(EventState.RESULTS_READY, election.state)
    Assert.assertEquals(results, election.getResultsForQuestionId(QUESTION.id))
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
  fun testHandleElectionOpen() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(OPENED_AT, election.startTimestamp)
    Assert.assertEquals(EventState.OPENED, election.state)
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
  fun testHandleElectionOpenInvalidState() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)

    // Set the election to a closed state and check that opening the election fails
    val closedElection = OPEN_BALLOT_ELECTION.builder().setState(EventState.CLOSED).build()
    electionRepo.updateElection(closedElection)
    Assert.assertThrows(InvalidStateException::class.java) { handleElectionOpen(closedElection) }

    // Set the election to an opened state and check that opening the election fails
    val openedElection = OPEN_BALLOT_ELECTION.builder().setState(EventState.OPENED).build()
    electionRepo.updateElection(closedElection)
    Assert.assertThrows(InvalidStateException::class.java) { handleElectionOpen(openedElection) }

    // Set the election to a result ready state and check that opening the election fails
    val resultsReadyElection =
      OPEN_BALLOT_ELECTION.builder().setState(EventState.RESULTS_READY).build()
    electionRepo.updateElection(closedElection)
    Assert.assertThrows(InvalidStateException::class.java) {
      handleElectionOpen(resultsReadyElection)
    }
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
  fun testHandleElectionEnd() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)
    handleElectionEnd()

    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(EventState.CLOSED, election.state)
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteWithOpenBallotScenario() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    // Handle the votes of two different senders
    handleCastVote(VOTE1, SENDER_KEY, OPENED_AT)
    handleCastVote(VOTE2, ATTENDEE_KEY, OPENED_AT)

    // The expected hash is made on the sorted vote ids
    val voteIds = Stream.of(VOTE1, VOTE2).map(Vote::id).sorted().collect(Collectors.toList())
    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(hash(*voteIds.toTypedArray()), election.computeRegisteredVotesHash())
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteOnlyKeepsLastVote() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    // Handle the votes of two different senders. One sender sends two votes, and the first should
    // be discarded
    handleCastVote(VOTE1, SENDER_KEY, OPENED_AT)
    handleCastVote(VOTE2, ATTENDEE_KEY, OPENED_AT + 1)
    handleCastVote(VOTE3, ATTENDEE_KEY, OPENED_AT + 2)

    // The expected hash is made on the sorted vote ids (check that vote2 was discarded)
    val voteIds = Stream.of(VOTE1, VOTE3).map(Vote::id).sorted().collect(Collectors.toList())
    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(hash(*voteIds.toTypedArray()), election.computeRegisteredVotesHash())
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteDiscardsStaleVote() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    // Handle the votes of two different senders. One sender sends two votes, and the second should
    // be discarded as it has an older creation date
    handleCastVote(VOTE1, SENDER_KEY, OPENED_AT)
    handleCastVote(VOTE2, ATTENDEE_KEY, OPENED_AT + 2)
    handleCastVote(VOTE3, ATTENDEE_KEY, OPENED_AT + 1)

    // The expected hash is made on the sorted vote ids (check that vote3 was discarded)
    val voteIds = Stream.of(VOTE1, VOTE2).map(Vote::id).sorted().collect(Collectors.toList())
    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(hash(*voteIds.toTypedArray()), election.computeRegisteredVotesHash())
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteFailsOnPreviousMessageDataNull() {
    // This test checks that the handler fails if the messageMap of the election already has a
    // message (previously sent by the same sender) that contains null data.
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    // Create an invalid message with null data
    Assert.assertThrows(IllegalArgumentException::class.java) {
      MessageGeneral(SENDER_KEY, null, gson)
    }
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteFailsOnPreviousMessageDataInvalid() {
    // This test checks that the handler fails if the messageMap of the election already has a
    // message (previously sent by the same sender) that contains data that is not a CastVote.
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    // Create an invalid message with data that is not a CastVote and add it to the message repo
    val invalidData = MessageGeneral(SENDER_KEY, CREATE_LAO, gson)
    messageRepo.addMessage(invalidData, isContentNeeded = true, toPersist = true)

    // Update the messageMap in this election to contain the invalid message
    val prevElection = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    val updatedElection =
      prevElection.builder().updateMessageMap(SENDER, invalidData.messageId).build()
    electionRepo.updateElection(updatedElection)

    // Check that handling the message fails
    Assert.assertThrows(DataHandlingException::class.java) {
      handleCastVote(VOTE1, SENDER_KEY, OPENED_AT + 1)
    }
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteIgnoresVoteOnClosedElection() {
    handleElectionSetup(OPEN_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(OPEN_BALLOT_ELECTION, ELECTION_KEY)
    handleElectionOpen(OPEN_BALLOT_ELECTION)

    val messageID = handleCastVote(VOTE1, SENDER_KEY, OPENED_AT)
    handleElectionEnd()
    handleCastVote(VOTE2, SENDER_KEY, END_AT + 1)

    // The expected message map kept the first vote
    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    val expectedMessageMap = HashMap<PublicKey, MessageID>()
    expectedMessageMap[SENDER] = messageID

    Assert.assertEquals(expectedMessageMap, election.messageMap)
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun castVoteWithSecretBallotScenario() {
    val keys = generateKeyPair()
    val pubKey = keys.encryptionScheme
    val encodedKey = Base64URLData(pubKey.publicKey.toBytes())
    val vote1 = EncryptedVote(QUESTION.id, "0", false, null, ELECTION_ID)
    val vote2 = EncryptedVote(QUESTION.id, "1", false, null, ELECTION_ID)

    handleElectionSetup(SECRET_BALLOT_ELECTION, LAO_CHANNEL)
    handleElectionKey(SECRET_BALLOT_ELECTION, encodedKey.encoded)
    handleElectionOpen(SECRET_BALLOT_ELECTION)
    handleCastVote(vote1, SENDER_KEY, OPENED_AT)
    handleCastVote(vote2, ATTENDEE_KEY, OPENED_AT)

    // The expected hash is made on the sorted vote ids
    val voteIds = Stream.of(vote1, vote2).map(Vote::id).sorted().collect(Collectors.toList())
    val election = electionRepo.getElectionByChannel(OPEN_BALLOT_ELECTION.channel)
    Assert.assertEquals(hash(*voteIds.toTypedArray()), election.computeRegisteredVotesHash())
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val ATTENDEE_KEY = Base64DataUtils.generateKeyPair()
    private const val LAO_NAME = "lao name"
    private val CREATION = Instant.now().epochSecond - 100
    private val LAO_ID = generateLaoId(SENDER, CREATION, LAO_NAME)
    private val CREATE_LAO = CreateLao(LAO_ID, LAO_NAME, CREATION, SENDER, ArrayList())
    private val LAO = Lao(LAO_NAME, SENDER, CREATION)
    private val LAO_CHANNEL = Channel.ROOT.subChannel(LAO.id)
    private val CREATED_AT = CREATION + 10 // 10 seconds later
    private val STARTED_AT = CREATION + 20 // 20 seconds later
    private val OPENED_AT = CREATION + 30 // 30 seconds later
    private val END_AT = CREATION + 60 // 60 seconds later
    private const val ELECTION_NAME = "Election Name"
    private val ELECTION_ID = generateElectionSetupId(LAO.id, CREATED_AT, ELECTION_NAME)
    private const val OPTION_1 = "Yes"
    private const val OPTION_2 = "No"
    private val QUESTION =
      ElectionQuestion(
        ELECTION_ID,
        Question("Does this work ?", "Plurality", listOf(OPTION_1, OPTION_2), false)
      )
    private val VOTE1 = PlainVote(QUESTION.id, 0, false, null, ELECTION_ID)
    private val VOTE2 = PlainVote(QUESTION.id, 1, false, null, ELECTION_ID)
    private val VOTE3 = PlainVote(QUESTION.id, 0, false, null, ELECTION_ID)
    private val ELECTION_KEY = Base64DataUtils.generateRandomBase64String()
    private val OPEN_BALLOT_ELECTION =
      ElectionBuilder(LAO.id, CREATED_AT, ELECTION_NAME)
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .setElectionQuestions(listOf(QUESTION))
        .setStart(STARTED_AT)
        .setEnd(END_AT)
        .build()
    private val SECRET_BALLOT_ELECTION =
      OPEN_BALLOT_ELECTION.builder().setElectionVersion(ElectionVersion.SECRET_BALLOT).build()
  }
}
