package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao
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
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler.Companion.closeRollCallWitnessMessage
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler.Companion.createRollCallWitnessMessage
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler.Companion.openRollCallWitnessMessage
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
class RollCallHandlerTest {
  private lateinit var rollCallRepo: RollCallRepository
  private lateinit var witnessingRepository: WitnessingRepository
  private lateinit var messageHandler: MessageHandler
  private lateinit var gson: Gson
  private lateinit var rollCall: RollCall

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var electionRepo: ElectionRepository

  @Mock lateinit var meetingRepo: MeetingRepository

  @Mock lateinit var digitalCashRepo: DigitalCashRepository

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var rollCallDao: RollCallDao

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

    Mockito.`when`(appDatabase.rollCallDao()).thenReturn(rollCallDao)
    Mockito.`when`(rollCallDao.getRollCallsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(rollCallDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())

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
    rollCallRepo = RollCallRepository(appDatabase, application)
    witnessingRepository =
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepo,
        electionRepo,
        meetingRepo,
        digitalCashRepo
      )
    val messageRepo = MessageRepository(appDatabase, ApplicationProvider.getApplicationContext())
    val dataRegistry = buildRegistry(laoRepo, keyManager, rollCallRepo, witnessingRepository)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)

    // Create one LAO
    LAO = Lao(CREATE_LAO.name, CREATE_LAO.organizer, CREATE_LAO.creation)
    LAO.lastModified = LAO.creation

    // Create one Roll Call and add it to the roll call repo
    val now = Instant.now().epochSecond
    rollCall =
      RollCall(
        LAO.id,
        LAO.id,
        "roll call 1",
        now,
        now + 1,
        now + 2,
        EventState.CREATED,
        LinkedHashSet(),
        "somewhere",
        "desc"
      )
    rollCallRepo.updateRollCall(LAO.id, rollCall)

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO)

    // Add the CreateLao message to the LAORepository
    val createLaoMessage = MessageGeneral(SENDER_KEY, CREATE_LAO, gson)
    messageRepo.addMessage(createLaoMessage, isContentNeeded = false, toPersist = false)
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
  fun testHandleCreateRollCall() {
    // Create the create Roll Call message
    val createRollCall =
      CreateRollCall(
        "roll call 2",
        rollCall.creation,
        rollCall.startTimestamp,
        rollCall.end,
        rollCall.location,
        rollCall.description,
        CREATE_LAO.id
      )
    val message = MessageGeneral(SENDER_KEY, createRollCall, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)

    // Check the new Roll Call is present with state CREATED and the correct ID
    val rollCallCheck = rollCallRepo.getRollCallWithId(LAO.id, createRollCall.id)
    Assert.assertEquals(EventState.CREATED, rollCallCheck.state)
    Assert.assertEquals(createRollCall.id, rollCallCheck.id)

    // Check the WitnessMessage has been created
    val witnessMessage = witnessingRepository.getWitnessMessage(LAO.id, message.messageId)
    Assert.assertTrue(witnessMessage.isPresent)

    // Check the Witness message contains the expected title and description
    val expectedMessage = createRollCallWitnessMessage(message.messageId, rollCallCheck)
    Assert.assertEquals(expectedMessage.title, witnessMessage.get().title)
    Assert.assertEquals(expectedMessage.description, witnessMessage.get().description)
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
  fun testHandleOpenRollCall() {
    // Create the open Roll Call message
    val openRollCall =
      OpenRollCall(CREATE_LAO.id, rollCall.id, rollCall.startTimestamp, EventState.CREATED)
    val message = MessageGeneral(SENDER_KEY, openRollCall, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)

    // Check the Roll Call is present with state OPENED and the correct ID
    val rollCallCheck = rollCallRepo.getRollCallWithId(LAO.id, openRollCall.updateId)
    Assert.assertEquals(EventState.OPENED, rollCallCheck.state)
    Assert.assertTrue(rollCallCheck.isOpen)
    Assert.assertEquals(openRollCall.updateId, rollCallCheck.id)

    // Check the WitnessMessage has been created
    val witnessMessage = witnessingRepository.getWitnessMessage(LAO.id, message.messageId)
    Assert.assertTrue(witnessMessage.isPresent)

    // Check the Witness message contains the expected title and description
    val expectedMessage = openRollCallWitnessMessage(message.messageId, rollCallCheck)
    Assert.assertEquals(expectedMessage.title, witnessMessage.get().title)
    Assert.assertEquals(expectedMessage.description, witnessMessage.get().description)
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
  fun testBlockOpenRollCall() {
    // Assert that a Roll Call can be opened
    Assert.assertTrue(rollCallRepo.canOpenRollCall(LAO.id))

    // Create the open Roll Call message
    val openRollCall =
      OpenRollCall(CREATE_LAO.id, rollCall.id, rollCall.startTimestamp, EventState.CREATED)
    val messageOpen = MessageGeneral(SENDER_KEY, openRollCall, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, messageOpen)

    // Check that no new Roll Call can be opened
    Assert.assertFalse(rollCallRepo.canOpenRollCall(LAO.id))

    // Create the close Roll Call message
    val closeRollCall = CloseRollCall(CREATE_LAO.id, rollCall.id, rollCall.end, ArrayList())
    val messageClose = MessageGeneral(SENDER_KEY, closeRollCall, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, messageClose)

    // Check that now new Roll Calls can be opened
    Assert.assertTrue(rollCallRepo.canOpenRollCall(LAO.id))
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
  fun testHandleCloseRollCall() {
    // Create the open Roll Call message
    val openRollCall =
      OpenRollCall(CREATE_LAO.id, rollCall.id, rollCall.startTimestamp, EventState.CREATED)

    // Call the message handler
    messageHandler.handleMessage(
      messageSender,
      LAO_CHANNEL,
      MessageGeneral(SENDER_KEY, openRollCall, gson)
    )

    // Create the close Roll Call message
    val closeRollCall = CloseRollCall(CREATE_LAO.id, rollCall.id, rollCall.end, ArrayList())
    val message = MessageGeneral(SENDER_KEY, closeRollCall, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)

    // Check the Roll Call is present with state CLOSED and the correct ID
    val rollCallCheck = rollCallRepo.getRollCallWithId(LAO.id, closeRollCall.updateId)
    Assert.assertEquals(EventState.CLOSED, rollCallCheck.state)
    Assert.assertTrue(rollCallCheck.isClosed)
    Assert.assertEquals(closeRollCall.updateId, rollCallCheck.id)

    // Check the WitnessMessage has been created
    val witnessMessage = witnessingRepository.getWitnessMessage(LAO.id, message.messageId)
    Assert.assertTrue(witnessMessage.isPresent)

    // Check the Witness message contains the expected title and description
    val expectedMessage = closeRollCallWitnessMessage(message.messageId, rollCallCheck)
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
  }
}
