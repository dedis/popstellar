package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.GreetLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.github.dedis.popstellar.model.objects.PendingUpdate
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.ServerRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.InvalidSignatureException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.data.LaoHandler.Companion.updateLaoNameWitnessMessage
import com.github.dedis.popstellar.utility.handler.data.LaoHandler.Companion.updateLaoWitnessesWitnessMessage
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class LaoHandlerTest {
  private lateinit var laoRepo: LAORepository
  private lateinit var witnessingRepository: WitnessingRepository
  private lateinit var consensusRepo: ConsensusRepository
  private lateinit var messageHandler: MessageHandler
  private lateinit var serverRepository: ServerRepository
  private lateinit var gson: Gson
  private lateinit var lao: Lao
  private lateinit var createLaoMessage: MessageGeneral

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var rollCallRepo: RollCallRepository

  @Mock lateinit var meetingRepo: MeetingRepository

  @Mock lateinit var electionRepo: ElectionRepository

  @Mock lateinit var digitalCashRepo: DigitalCashRepository

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var witnessingDao: WitnessingDao

  @Mock lateinit var witnessDao: WitnessDao

  @Mock lateinit var pendingDao: PendingDao

  @Mock lateinit var messageSender: MessageSender

  @Mock lateinit var keyManager: KeyManager

  @Before
  @Throws(GeneralSecurityException::class, IOException::class)
  fun setup() {
    MockitoAnnotations.openMocks(this)
    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.lenient().`when`(keyManager.mainKeyPair).thenReturn(SENDER_KEY1)
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(SENDER1)
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

    laoRepo = LAORepository(appDatabase, application)
    val messageRepo = MessageRepository(appDatabase, application)
    serverRepository = ServerRepository()
    witnessingRepository =
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepo,
        electionRepo,
        meetingRepo,
        digitalCashRepo
      )
    consensusRepo = ConsensusRepository()

    val dataRegistry =
      buildRegistry(laoRepo, witnessingRepository, messageRepo, keyManager, serverRepository)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)

    // Create one LAO and add it to the LAORepository
    lao = Lao(CREATE_LAO1.name, CREATE_LAO1.organizer, CREATE_LAO1.creation)
    lao.lastModified = lao.creation
    laoRepo.updateLao(lao)

    // Add the CreateLao message to the LAORepository
    createLaoMessage = MessageGeneral(SENDER_KEY1, CREATE_LAO1, gson)
    messageRepo.addMessage(createLaoMessage, isContentNeeded = true, toPersist = true)
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
  fun testHandleCreateLaoOrganizer() {
    // Create the message (new CreateLao) and call the message handler
    val message = MessageGeneral(SENDER_KEY1, CREATE_LAO2, gson)
    messageHandler.handleMessage(messageSender, LAO_CHANNEL2, message)

    // Get expected results
    val resultLao = laoRepo.getLaoByChannel(LAO_CHANNEL2)
    val expectedName = CREATE_LAO2.name
    val expectedOrganizer = CREATE_LAO2.organizer
    val expectedCreation = CREATE_LAO2.creation
    val expectedID = generateLaoId(expectedOrganizer, expectedCreation, expectedName)

    // Check that the expected LAO was created in the LAO repo
    Assert.assertEquals(LAO_CHANNEL2, resultLao.channel)
    Assert.assertEquals(expectedID, resultLao.id)
    Assert.assertEquals(expectedName, resultLao.name)
    Assert.assertEquals(expectedCreation, resultLao.lastModified)
    Assert.assertEquals(expectedCreation, resultLao.creation)
    Assert.assertEquals(expectedOrganizer, resultLao.organizer)
    Assert.assertTrue(witnessingRepository.areWitnessesEmpty(LAO_CHANNEL2.extractLaoId()))
    Assert.assertTrue(witnessingRepository.areWitnessMessagesEmpty(LAO_CHANNEL3.extractLaoId()))
    Assert.assertNull(resultLao.modificationId)
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
  fun testHandleCreateLaoWitness() {
    // Main public key is a witness now, and not the organizer
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(SENDER2)

    // Create the message (CreateLao with witnesses) and call the message handler
    val message = MessageGeneral(SENDER_KEY1, CREATE_LAO3, gson)
    messageHandler.handleMessage(messageSender, LAO_CHANNEL3, message)

    // Get expected results
    val resultLao = laoRepo.getLaoByChannel(LAO_CHANNEL3)
    val expectedName = CREATE_LAO3.name
    val expectedOrganizer = CREATE_LAO3.organizer
    val expectedCreation = CREATE_LAO3.creation
    val expectedID = generateLaoId(expectedOrganizer, expectedCreation, expectedName)

    // Check that the expected LAO was created in the LAO repo
    Assert.assertEquals(LAO_CHANNEL3, resultLao.channel)
    Assert.assertEquals(expectedID, resultLao.id)
    Assert.assertEquals(expectedName, resultLao.name)
    Assert.assertEquals(expectedCreation, resultLao.lastModified)
    Assert.assertEquals(expectedCreation, resultLao.creation)
    Assert.assertEquals(expectedOrganizer, resultLao.organizer)
    Assert.assertEquals(
      HashSet(WITNESSES),
      witnessingRepository.getWitnesses(LAO_CHANNEL3.extractLaoId())
    )
    Assert.assertTrue(witnessingRepository.areWitnessMessagesEmpty(LAO_CHANNEL3.extractLaoId()))
    Assert.assertNull(resultLao.modificationId)
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
  fun testHandleUpdateLaoNewName() {
    // Create the update LAO message with new LAO name
    val updateLao =
      UpdateLao(SENDER1, CREATE_LAO1.creation, "new name", Instant.now().epochSecond, HashSet())
    val message = MessageGeneral(SENDER_KEY1, updateLao, gson)

    // Create the expected WitnessMessage
    val expectedMessage = updateLaoNameWitnessMessage(message.messageId, updateLao, LaoView(lao))

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)

    // Check the WitnessMessage has been created
    val witnessMessage =
      witnessingRepository.getWitnessMessage(LAO_CHANNEL1.extractLaoId(), message.messageId)
    Assert.assertTrue(witnessMessage.isPresent)
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
  fun testHandleUpdateLaoNewWitness() {
    // Set LAO to have one witness
    consensusRepo.initKeyToNode(lao.id, HashSet(WITNESS))

    // Create UpdateLao with updated witness set
    val updateLao =
      UpdateLao(
        SENDER1,
        CREATE_LAO1.creation,
        CREATE_LAO1.name,
        Instant.now().epochSecond,
        HashSet(WITNESSES)
      )
    val message = MessageGeneral(SENDER_KEY1, updateLao, gson)

    // Create the expected WitnessMessage and PendingUpdate
    val expectedMessage =
      updateLaoWitnessesWitnessMessage(message.messageId, updateLao, LaoView(lao))
    val pendingUpdate = PendingUpdate(updateLao.lastModified, message.messageId)
    val expectedPendingUpdateSet = HashSet<PendingUpdate>()
    expectedPendingUpdateSet.add(pendingUpdate)

    // Call the handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)

    // Check the WitnessMessage has been created
    val witnessMessage =
      witnessingRepository.getWitnessMessage(LAO_CHANNEL1.extractLaoId(), message.messageId)
    Assert.assertTrue(witnessMessage.isPresent)
    Assert.assertEquals(expectedMessage.title, witnessMessage.get().title)
    Assert.assertEquals(expectedMessage.description, witnessMessage.get().description)

    // Check the PendingUpdate has been added
    Assert.assertEquals(
      expectedPendingUpdateSet,
      laoRepo.getLaoByChannel(LAO_CHANNEL1).pendingUpdates
    )
  }

  @Test
  fun testHandleUpdateLaoOldInfo() {
    // Create UpdateLao with no updated name or witness set
    val updateLao =
      UpdateLao(
        SENDER1,
        CREATE_LAO1.creation,
        CREATE_LAO1.name,
        Instant.now().epochSecond,
        HashSet()
      )
    val message = MessageGeneral(SENDER_KEY1, updateLao, gson)

    // Check that handling the message fails
    Assert.assertThrows(DataHandlingException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)
    }
  }

  @Test
  fun testHandleUpdateLaoStale() {
    // Create a update LAO message with last modified time older than the current LAO last modified
    // time
    lao.lastModified = CREATE_LAO1.creation + 10
    val updateLao1 =
      UpdateLao(SENDER1, CREATE_LAO1.creation, "new lao name", CREATE_LAO1.creation + 5, HashSet())
    val message = MessageGeneral(SENDER_KEY1, updateLao1, gson)

    // Check that handling the older message fails
    Assert.assertThrows(DataHandlingException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)
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
  fun testHandleStateLaoOrganizer() {
    // Create a valid list of modification signatures
    val modificationSignatures = getValidModificationSignatures(createLaoMessage)

    // Create the state LAO message
    val stateLao =
      StateLao(
        CREATE_LAO1.id,
        CREATE_LAO1.name,
        CREATE_LAO1.creation,
        Instant.now().epochSecond,
        CREATE_LAO1.organizer,
        createLaoMessage.messageId,
        HashSet(),
        modificationSignatures
      )
    val message = MessageGeneral(SENDER_KEY1, stateLao, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)

    // Check the LAO last modification time and ID was updated
    Assert.assertEquals(stateLao.lastModified, laoRepo.getLaoByChannel(LAO_CHANNEL1).lastModified)
    Assert.assertEquals(
      stateLao.modificationId,
      laoRepo.getLaoByChannel(LAO_CHANNEL1).modificationId
    )
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
  fun testHandleStateLaoWitness() {
    // Main public key is a witness now, and not the organizer
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(SENDER2)

    // Create the state LAO message with one witness that has the main public key
    val stateLao =
      StateLao(
        CREATE_LAO1.id,
        CREATE_LAO1.name,
        CREATE_LAO1.creation,
        Instant.now().epochSecond,
        CREATE_LAO1.organizer,
        createLaoMessage.messageId,
        HashSet(WITNESS),
        ArrayList()
      )
    val message = MessageGeneral(SENDER_KEY1, stateLao, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)

    // Check the LAO last modification time and ID was updated
    Assert.assertEquals(stateLao.lastModified, laoRepo.getLaoByChannel(LAO_CHANNEL1).lastModified)
    Assert.assertEquals(
      stateLao.modificationId,
      laoRepo.getLaoByChannel(LAO_CHANNEL1).modificationId
    )
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
  fun testHandleStateLaoRemovesStalePendingUpdates() {
    // Create a list of 2 pending updates: one newer and one older than the stateLao message
    val targetTime = CREATE_LAO1.creation + 5
    val oldPendingUpdate = Mockito.mock(PendingUpdate::class.java)
    val newPendingUpdate = Mockito.mock(PendingUpdate::class.java)
    Mockito.`when`(oldPendingUpdate.modificationTime).thenReturn(targetTime - 1)
    Mockito.`when`(newPendingUpdate.modificationTime).thenReturn(targetTime + 1)
    val pendingUpdates: MutableSet<PendingUpdate> =
      HashSet(listOf(oldPendingUpdate, newPendingUpdate))

    // Add the list of pending updates to the LAO
    val createdLao = laoRepo.getLaoByChannel(LAO_CHANNEL1)
    createdLao.pendingUpdates = pendingUpdates

    // Create the stateLao message
    val stateLao =
      StateLao(
        CREATE_LAO1.id,
        CREATE_LAO1.name,
        CREATE_LAO1.creation,
        targetTime,
        CREATE_LAO1.organizer,
        createLaoMessage.messageId,
        HashSet(),
        ArrayList()
      )
    val stateMessage = MessageGeneral(SENDER_KEY1, stateLao, gson)

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, stateMessage)

    // The old pending update is removed in the expected pending list
    val expectedPending: Set<PendingUpdate> = HashSet(listOf(newPendingUpdate))
    Assert.assertEquals(expectedPending, laoRepo.getLaoByChannel(LAO_CHANNEL1).pendingUpdates)
  }

  @Test
  fun testHandleStateLaoInvalidMessageId() {
    // Create some message that has an invalid ID
    val createLaoMessage2 = MessageGeneral(SENDER_KEY1, CREATE_LAO2, gson)

    // Create the state LAO message
    val stateLao =
      StateLao(
        CREATE_LAO1.id,
        CREATE_LAO1.name,
        CREATE_LAO1.creation,
        Instant.now().epochSecond,
        CREATE_LAO1.organizer,
        createLaoMessage2.messageId,
        HashSet(),
        ArrayList()
      )
    val message = MessageGeneral(SENDER_KEY1, stateLao, gson)

    // Check that handling the message with invalid ID fails
    Assert.assertThrows(InvalidMessageIdException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)
    }
  }

  @Test
  fun testHandleStateLaoInvalidSignatures() {
    // Create a list with invalid modification signature
    val modificationSignatures = invalidModificationSignatures

    // Create the a state LAO message with invalid modification signatures
    val stateLao =
      StateLao(
        CREATE_LAO1.id,
        CREATE_LAO1.name,
        CREATE_LAO1.creation,
        Instant.now().epochSecond,
        CREATE_LAO1.organizer,
        createLaoMessage.messageId,
        HashSet(),
        modificationSignatures
      )
    val message = MessageGeneral(SENDER_KEY1, stateLao, gson)

    // Check that handling the message with invalid signatures fails
    Assert.assertThrows(InvalidSignatureException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)
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
  fun testGreetLao() {
    // Create the Greet Lao
    val greetLao = GreetLao(lao.id, RANDOM_KEY, RANDOM_ADDRESS, listOf(RANDOM_PEER))
    val message = MessageGeneral(SENDER_KEY1, greetLao, gson)

    // Call the handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL1, message)

    // Check that the server repository contains the key of the server
    Assert.assertEquals(RANDOM_ADDRESS, serverRepository.getServerByLaoId(lao.id).serverAddress)
    // Check that it contains the key as well
    Assert.assertEquals(PublicKey(RANDOM_KEY), serverRepository.getServerByLaoId(lao.id).publicKey)

    // Test that the handler throws an exception if the lao id does not match the current one
    val invalidId = generateLaoId(SENDER1, CREATE_LAO1.creation, "some name")
    val greetLaoInvalid = GreetLao(invalidId, RANDOM_KEY, RANDOM_ADDRESS, listOf(RANDOM_PEER))
    val messageInvalid = MessageGeneral(SENDER_KEY1, greetLaoInvalid, gson)

    Assert.assertThrows(IllegalArgumentException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL1, messageInvalid)
    }
  }

  companion object {
    private val SENDER_KEY1 = Base64DataUtils.generateKeyPair()
    private val SENDER_KEY2 = Base64DataUtils.generateKeyPair()
    private val SENDER1 = SENDER_KEY1.publicKey
    private val SENDER2 = SENDER_KEY2.publicKey
    private val CREATION = Instant.now().epochSecond - 10
    private const val NAME1 = "lao1"
    private const val NAME2 = "lao2"
    private const val NAME3 = "lao3"
    private val ID1 = generateLaoId(SENDER1, CREATION, NAME1)
    private val ID2 = generateLaoId(SENDER1, CREATION, NAME2)
    private val ID3 = generateLaoId(SENDER1, CREATION, NAME3)
    private val WITNESS: List<PublicKey> = ArrayList(listOf(SENDER2))
    private val WITNESSES: List<PublicKey> = ArrayList(listOf(SENDER1, SENDER2))
    private val CREATE_LAO1 = CreateLao(ID1, NAME1, CREATION, SENDER1, ArrayList())
    private val CREATE_LAO2 = CreateLao(ID2, NAME2, CREATION, SENDER1, ArrayList())
    private val CREATE_LAO3 = CreateLao(ID3, NAME3, CREATION, SENDER1, WITNESSES)
    private val LAO_CHANNEL1 = getLaoChannel(CREATE_LAO1.id)
    private val LAO_CHANNEL2 = getLaoChannel(CREATE_LAO2.id)
    private val LAO_CHANNEL3 = getLaoChannel(CREATE_LAO3.id)
    const val RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8"
    const val RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client"
    val RANDOM_PEER = PeerAddress("ws://128.0.0.2:8001/")

    private fun getValidModificationSignatures(
      messageGeneral: MessageGeneral
    ): List<PublicKeySignaturePair> {
      val validKeyPair: PublicKeySignaturePair =
        try {
          PublicKeySignaturePair(SENDER1, SENDER_KEY1.sign(messageGeneral.messageId))
        } catch (e: GeneralSecurityException) {
          throw RuntimeException(e)
        }
      return listOf(validKeyPair)
    }

    private val invalidModificationSignatures: List<PublicKeySignaturePair>
      get() {
        val invalidKeyPair =
          PublicKeySignaturePair(
            Base64DataUtils.generatePublicKey(),
            Base64DataUtils.generateSignature()
          )
        return ArrayList(listOf(invalidKeyPair))
      }
  }
}
