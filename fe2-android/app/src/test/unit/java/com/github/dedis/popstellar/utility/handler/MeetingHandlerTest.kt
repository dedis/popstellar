package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Meeting.Companion.generateCreateMeetingId
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownMeetingException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.data.MeetingHandler.Companion.createMeetingWitnessMessage
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
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
class MeetingHandlerTest {
  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var rollCallRepository: RollCallRepository

  @Mock lateinit var digitalCashRepo: DigitalCashRepository

  @Mock lateinit var electionRepo: ElectionRepository

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var meetingDao: MeetingDao

  @Mock lateinit var witnessingDao: WitnessingDao

  @Mock lateinit var witnessDao: WitnessDao

  @Mock lateinit var pendingDao: PendingDao

  @Mock lateinit var messageSender: MessageSender

  @Mock lateinit var keyManager: KeyManager

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @Before
  @Throws(
    GeneralSecurityException::class,
    IOException::class,
    KeyException::class,
    UnknownRollCallException::class
  )
  fun setup() {
    MockitoAnnotations.openMocks(this)
    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.lenient().`when`(keyManager.mainKeyPair).thenReturn(SENDER_KEY)
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(SENDER)
    Mockito.lenient()
      .`when`(keyManager.getValidPoPToken(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any()))
      .thenReturn(POP_TOKEN)

    Mockito.lenient().`when`(messageSender.subscribe(MockitoKotlinHelpers.any())).then {
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

    Mockito.`when`(appDatabase.meetingDao()).thenReturn(meetingDao)
    Mockito.`when`(meetingDao.getMeetingsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(meetingDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())

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
    meetingRepo = MeetingRepository(appDatabase, application)
    witnessingRepository =
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepository,
        electionRepo,
        meetingRepo,
        digitalCashRepo
      )
    val messageRepo = MessageRepository(appDatabase, application)

    val dataRegistry = buildRegistry(laoRepo, keyManager, meetingRepo, witnessingRepository)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)

    // Create one LAO
    LAO = Lao(CREATE_LAO.name, CREATE_LAO.organizer, CREATE_LAO.creation)
    LAO.lastModified = LAO.creation

    // Create one Roll Call and add it to the roll call repo
    val now = Instant.now().epochSecond
    val name = "NAME"
    val ID = generateCreateMeetingId(LAO.id, now, name)
    meeting = Meeting(ID, name, now, now + 1, now + 2, "", now, "", ArrayList())
    meetingRepo.updateMeeting(LAO.id, meeting)

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO)

    // Add the CreateLao message to the LAORepository
    val createLaoMessage = MessageGeneral(SENDER_KEY, CREATE_LAO, gson)
    messageRepo.addMessage(createLaoMessage, isContentNeeded = true, toPersist = true)
  }

  @Test
  @Throws(
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownMeetingException::class,
    UnknownWitnessMessageException::class
  )
  fun handleCreateMeetingTest() {
    // Create the create Meeting message
    val createMeeting =
      CreateMeeting(
        LAO.id,
        meeting.id,
        meeting.name,
        meeting.creation,
        meeting.location,
        meeting.startTimestamp,
        meeting.endTimestamp
      )
    val message = MessageGeneral(SENDER_KEY, createMeeting, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)

    // Check the new Meeting is present with state CREATED and the correct ID
    val meetingSearch = meetingRepo.getMeetingWithId(LAO.id, meeting.id)
    Assert.assertEquals(createMeeting.id, meetingSearch.id)

    // Check the WitnessMessage has been created
    val witnessMessage = witnessingRepository.getWitnessMessage(LAO.id, message.messageId)
    Assert.assertTrue(witnessMessage.isPresent)

    // Check the Witness message contains the expected title and description
    val expectedMessage = createMeetingWitnessMessage(message.messageId, meetingSearch)
    Assert.assertEquals(expectedMessage.title, witnessMessage.get().title)
    Assert.assertEquals(expectedMessage.description, witnessMessage.get().description)
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val POP_TOKEN = Base64DataUtils.generatePoPToken()
    private val CREATE_LAO = CreateLao("lao", SENDER, ArrayList())
    private val LAO_CHANNEL = getLaoChannel(CREATE_LAO.id)
    private lateinit var LAO: Lao
    private lateinit var meetingRepo: MeetingRepository
    private lateinit var witnessingRepository: WitnessingRepository
    private lateinit var messageHandler: MessageHandler
    private lateinit var gson: Gson
    private lateinit var meeting: Meeting
  }
}
