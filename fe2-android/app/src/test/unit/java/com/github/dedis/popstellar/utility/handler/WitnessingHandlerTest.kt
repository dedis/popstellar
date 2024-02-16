package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.RollCallRepository
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
import com.github.dedis.popstellar.utility.error.InvalidSignatureException
import com.github.dedis.popstellar.utility.error.InvalidWitnessingException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
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
class WitnessingHandlerTest {
  private lateinit var laoRepo: LAORepository
  private lateinit var consensusRepo: ConsensusRepository
  private lateinit var messageHandler: MessageHandler
  private lateinit var gson: Gson

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var rollCallRepo: RollCallRepository

  @Mock lateinit var digitalCashRepo: DigitalCashRepository

  @Mock lateinit var meetingRepo: MeetingRepository

  @Mock lateinit var electionRepo: ElectionRepository

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var witnessDao: WitnessDao

  @Mock lateinit var witnessingDao: WitnessingDao

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

    Mockito.lenient().`when`(keyManager.mainKeyPair).thenReturn(ORGANIZER_KEY)
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(ORGANIZER)
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
    val messageRepo = MessageRepository(appDatabase, application)
    val dataRegistry = buildRegistry(laoRepo, witnessingRepository, keyManager)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)

    // Create one LAO
    val LAO = Lao(CREATE_LAO.name, CREATE_LAO.organizer, CREATE_LAO.creation)
    LAO.lastModified = LAO.creation
    consensusRepo.initKeyToNode(LAO.id, HashSet(CREATE_LAO.witnesses))
    witnessingRepository.addWitnesses(LAO.id, HashSet(WITNESSES))
    witnessingRepository.addWitnessMessage(LAO.id, WITNESS_MESSAGE1)
    witnessingRepository.addWitnessMessage(LAO.id, WITNESS_MESSAGE2)

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO)

    // Add the CreateLao message to the LAORepository
    val createLaoMessage = MessageGeneral(ORGANIZER_KEY, CREATE_LAO, gson)
    messageRepo.addMessage(createLaoMessage, isContentNeeded = false, toPersist = false)
  }

  @Test
  @Throws(
    GeneralSecurityException::class,
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun testHandleWitnessMessageSignatureFromOrganizer() {
    // Create a valid witnessMessageSignature signed by the organizer
    val signature = ORGANIZER_KEY.sign(MESSAGE_ID1)
    val witnessMessageSignature = WitnessMessageSignature(MESSAGE_ID1, signature)
    val message = MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson)

    // Handle the witnessMessageSignature
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)

    // Check that the witness message in the lao repo was updated with the organizer public key
    val lao = laoRepo.getLaoByChannel(LAO_CHANNEL)
    val witnessMessage = witnessingRepository.getWitnessMessage(lao.id, MESSAGE_ID1)
    Assert.assertTrue(witnessMessage.isPresent)

    val expectedWitnesses = HashSet<PublicKey>()
    expectedWitnesses.add(ORGANIZER)
    Assert.assertEquals(expectedWitnesses, witnessMessage.get().witnesses)
  }

  @Test
  @Throws(
    GeneralSecurityException::class,
    UnknownElectionException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    DataHandlingException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun testHandleWitnessMessageSignatureFromWitness() {
    // Create a valid witnessMessageSignature signed by a witness
    val signature = WITNESS_KEY.sign(MESSAGE_ID2)
    val witnessMessageSignature = WitnessMessageSignature(MESSAGE_ID2, signature)
    val message = MessageGeneral(WITNESS_KEY, witnessMessageSignature, gson)

    // Handle the witnessMessageSignature
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)

    // Check that the witness message in the lao repo was updated with the witness public key
    val lao = laoRepo.getLaoByChannel(LAO_CHANNEL)
    val witnessMessage = witnessingRepository.getWitnessMessage(lao.id, MESSAGE_ID2)
    Assert.assertTrue(witnessMessage.isPresent)

    val expectedWitnesses = HashSet<PublicKey>()
    expectedWitnesses.add(WITNESS)
    Assert.assertEquals(expectedWitnesses, witnessMessage.get().witnesses)
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun testHandleWitnessMessageSignatureFromNonWitness() {
    // Create a witnessMessageSignature signed by a non witness
    val invalidKeyPair = Base64DataUtils.generateKeyPair()
    val signature = invalidKeyPair.sign(MESSAGE_ID1)
    val witnessMessageSignature = WitnessMessageSignature(MESSAGE_ID1, signature)
    val message = MessageGeneral(invalidKeyPair, witnessMessageSignature, gson)

    Assert.assertThrows(InvalidWitnessingException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)
    }
  }

  @Test
  fun testHandleWitnessMessageSignatureWithInvalidSignature() {
    // Create a witnessMessageSignature with an invalid signature
    val witnessMessageSignature =
      WitnessMessageSignature(MESSAGE_ID1, Base64DataUtils.generateSignature())
    val message = MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson)

    Assert.assertThrows(InvalidSignatureException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)
    }
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun testHandleWitnessMessageSignatureWithNonExistentWitnessMessage() {
    // Create a witnessMessageSignature for a witness message that does not exist in the lao
    val invalidMessageID = Base64DataUtils.generateMessageIDOtherThan(MESSAGE_ID1)
    val signature = ORGANIZER_KEY.sign(invalidMessageID)
    val witnessMessageSignature = WitnessMessageSignature(invalidMessageID, signature)
    val message = MessageGeneral(ORGANIZER_KEY, witnessMessageSignature, gson)

    Assert.assertThrows(InvalidWitnessingException::class.java) {
      messageHandler.handleMessage(messageSender, LAO_CHANNEL, message)
    }
  }

  companion object {
    private val ORGANIZER_KEY = Base64DataUtils.generateKeyPair()
    private val WITNESS_KEY = Base64DataUtils.generateKeyPair()
    private val ORGANIZER = ORGANIZER_KEY.publicKey
    private val WITNESS = WITNESS_KEY.publicKey
    private val WITNESSES: List<PublicKey> = ArrayList(listOf(ORGANIZER, WITNESS))
    private val POP_TOKEN = Base64DataUtils.generatePoPToken()
    private val CREATE_LAO = CreateLao("lao", ORGANIZER, WITNESSES)
    private val LAO_CHANNEL = getLaoChannel(CREATE_LAO.id)
    private val MESSAGE_ID1 = Base64DataUtils.generateMessageID()
    private val MESSAGE_ID2 = Base64DataUtils.generateMessageIDOtherThan(MESSAGE_ID1)
    private val WITNESS_MESSAGE1 = WitnessMessage(MESSAGE_ID1)
    private val WITNESS_MESSAGE2 = WitnessMessage(MESSAGE_ID2)
    private lateinit var witnessingRepository: WitnessingRepository
  }
}
