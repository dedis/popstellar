package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusKey
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPrepare
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPromise
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPropose
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.ElectInstance.Companion.generateConsensusId
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Optional
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.internal.util.collections.Sets

@RunWith(AndroidJUnit4::class)
class ConsensusHandlerTest {
  private lateinit var consensusRepo: ConsensusRepository
  private lateinit var messageHandler: MessageHandler
  private lateinit var gson: Gson
  private lateinit var electMsg: MessageGeneral
  private lateinit var messageId: MessageID

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var messageSender: MessageSender

  @Mock lateinit var keyManager: KeyManager

  @Before
  @Throws(
    GeneralSecurityException::class,
    DataHandlingException::class,
    IOException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  fun setup() {
    MockitoAnnotations.openMocks(this)
    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.lenient().`when`(keyManager.mainKeyPair).thenReturn(ORGANIZER_KEY)
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(ORGANIZER)
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

    val laoRepo = LAORepository(appDatabase, application)
    consensusRepo = ConsensusRepository()
    val messageRepo = MessageRepository(appDatabase, application)
    val dataRegistry = buildRegistry(laoRepo, keyManager, consensusRepo)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)
    val channel = getLaoChannel(LAO_ID)
    val createLaoMessage = getMsg(ORGANIZER_KEY, CREATE_LAO)

    messageHandler.handleMessage(messageSender, channel, createLaoMessage)
    electMsg = getMsg(NODE_2_KEY, elect)
    messageId = electMsg.messageId
  }

  /**
   * Create a MessageGeneral containing the given data, with the given public key sender
   *
   * @param key public key of sender
   * @param data the data to encapsulated
   * @return a MessageGeneral
   */
  private fun getMsg(key: KeyPair, data: Data): MessageGeneral {
    return MessageGeneral(key, data, gson)
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
  fun handleConsensusTests() {
    // each test need to be run one after another
    handleConsensusElectTest()
    handleConsensusElectAcceptTest()
    handleConsensusLearnTest()
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
  fun handleConsensusFailure() {
    // handle an elect from node2 then handle a failure for this elect
    // the state of the node2 for this instanceId should be FAILED
    val failure = ConsensusFailure(INSTANCE_ID, messageId, CREATION_TIME)
    val failureMsg = getMsg(ORGANIZER_KEY, failure)

    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electMsg)
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, failureMsg)

    val electInstanceOpt = consensusRepo.getElectInstance(LAO_ID, electMsg.messageId)
    Assert.assertTrue(electInstanceOpt.isPresent)

    val electInstance = electInstanceOpt.get()
    Assert.assertEquals(ElectInstance.State.FAILED, electInstance.state)

    val node2 = consensusRepo.getNodeByLao(LAO_ID, NODE_2)
    Assert.assertNotNull(node2)
    Assert.assertEquals(ElectInstance.State.FAILED, node2!!.getState(INSTANCE_ID))
  }

  // handle an elect from node2
  // This should add an attempt from node2 to start a consensus (in this case for starting an
  // election)
  @Throws(
    DataHandlingException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleConsensusElectTest() {
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electMsg)

    val electInstanceOpt = consensusRepo.getElectInstance(LAO_ID, electMsg.messageId)
    Assert.assertTrue(electInstanceOpt.isPresent)

    val electInstance = electInstanceOpt.get()

    Assert.assertEquals(electMsg.messageId, electInstance.messageId)
    Assert.assertEquals(NODE_2, electInstance.proposer)
    Assert.assertEquals(CONSENSUS_CHANNEL, electInstance.channel)
    Assert.assertEquals(CREATION_TIME, electInstance.creation)
    Assert.assertEquals(VALUE, electInstance.value)
    Assert.assertEquals(KEY, electInstance.key)
    Assert.assertTrue(electInstance.acceptorsToMessageId.isEmpty())
    Assert.assertEquals(Sets.newSet(ORGANIZER, NODE_2, NODE_3), electInstance.nodes)

    val messageIdToElectInstance = consensusRepo.getMessageIdToElectInstanceByLao(LAO_ID)
    Assert.assertEquals(1, messageIdToElectInstance.size.toLong())
    Assert.assertEquals(electInstance, messageIdToElectInstance[electInstance.messageId])
    Assert.assertEquals(3, consensusRepo.getNodes(LAO_ID).size.toLong())

    val organizer = consensusRepo.getNodeByLao(LAO_ID, ORGANIZER)
    val node2 = consensusRepo.getNodeByLao(LAO_ID, NODE_2)
    val node3 = consensusRepo.getNodeByLao(LAO_ID, NODE_3)

    Assert.assertNotNull(organizer)
    Assert.assertNotNull(node2)
    Assert.assertNotNull(node3)

    val organizerElectInstance = organizer!!.getLastElectInstance(INSTANCE_ID)
    val node2ElectInstance = node2!!.getLastElectInstance(INSTANCE_ID)
    val node3ElectInstance = node3!!.getLastElectInstance(INSTANCE_ID)

    Assert.assertEquals(Optional.empty<Any>(), organizerElectInstance)
    Assert.assertTrue(node2ElectInstance.isPresent)
    Assert.assertEquals(electInstance, node2ElectInstance.get())
    Assert.assertEquals(Optional.empty<Any>(), node3ElectInstance)
  }

  // handle an electAccept from node3 for the elect of node2
  // This test need be run after the elect message was handled, else the messageId would be invalid
  @Throws(
    DataHandlingException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleConsensusElectAcceptTest() {
    val electAccept = ConsensusElectAccept(INSTANCE_ID, messageId, true)
    val electAcceptMsg = getMsg(NODE_3_KEY, electAccept)

    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electAcceptMsg)

    val electInstanceOpt = consensusRepo.getElectInstance(LAO_ID, electMsg.messageId)
    Assert.assertTrue(electInstanceOpt.isPresent)

    val electInstance = electInstanceOpt.get()
    val acceptorsToMessageId = electInstance.acceptorsToMessageId

    Assert.assertEquals(1, acceptorsToMessageId.size.toLong())
    Assert.assertEquals(electAcceptMsg.messageId, acceptorsToMessageId[NODE_3])
    Assert.assertEquals(3, consensusRepo.getNodes(LAO_ID).size.toLong())

    val organizer = consensusRepo.getNodeByLao(LAO_ID, ORGANIZER)
    val node2 = consensusRepo.getNodeByLao(LAO_ID, NODE_2)
    val node3 = consensusRepo.getNodeByLao(LAO_ID, NODE_3)

    Assert.assertNotNull(organizer)
    Assert.assertNotNull(node2)
    Assert.assertNotNull(node3)

    val organizerAcceptedMsg = organizer!!.getAcceptedMessageIds()
    val node2AcceptedMsg = node2!!.getAcceptedMessageIds()
    val node3AcceptedMsg = node3!!.getAcceptedMessageIds()

    Assert.assertTrue(organizerAcceptedMsg.isEmpty())
    Assert.assertTrue(node2AcceptedMsg.isEmpty())
    Assert.assertEquals(Sets.newSet(electMsg.messageId), node3AcceptedMsg)
  }

  // handle a learn from node3 for the elect of node2
  // This test need be run after the elect message was handled, else the messageId would be invalid
  @Throws(
    DataHandlingException::class,
    UnknownLaoException::class,
    UnknownRollCallException::class,
    UnknownElectionException::class,
    NoRollCallException::class,
    UnknownWitnessMessageException::class
  )
  private fun handleConsensusLearnTest() {
    val learn = ConsensusLearn(INSTANCE_ID, messageId, CREATION_TIME, true, emptyList())
    val learnMsg = getMsg(NODE_3_KEY, learn)

    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, learnMsg)

    val electInstanceOpt = consensusRepo.getElectInstance(LAO_ID, electMsg.messageId)
    Assert.assertTrue(electInstanceOpt.isPresent)

    val electInstance = electInstanceOpt.get()
    Assert.assertEquals(ElectInstance.State.ACCEPTED, electInstance.state)
  }

  @Test
  fun handleConsensusWithInvalidMessageIdTest() {
    // When an invalid instance id is used in handler for elect_accept and learn,
    // it should throw an InvalidMessageIdException
    val electAcceptInvalid = ConsensusElectAccept(INSTANCE_ID, INVALID_MSG_ID, true)
    val learnInvalid = ConsensusLearn(INSTANCE_ID, INVALID_MSG_ID, CREATION_TIME, true, emptyList())
    val failureInvalid = ConsensusFailure(INSTANCE_ID, INVALID_MSG_ID, CREATION_TIME)
    val electAcceptInvalidMsg = getMsg(ORGANIZER_KEY, electAcceptInvalid)
    val learnInvalidMsg = getMsg(ORGANIZER_KEY, learnInvalid)
    val failureMsg = getMsg(ORGANIZER_KEY, failureInvalid)

    Assert.assertThrows(InvalidMessageIdException::class.java) {
      messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, electAcceptInvalidMsg)
    }
    Assert.assertThrows(InvalidMessageIdException::class.java) {
      messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, learnInvalidMsg)
    }
    Assert.assertThrows(InvalidMessageIdException::class.java) {
      messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, failureMsg)
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
  fun handleConsensusDoNothingOnBackendMessageTest() {
    val mockLAORepository = Mockito.mock(LAORepository::class.java)
    val mockConsensusRepo = Mockito.mock(ConsensusRepository::class.java)

    val prepare = ConsensusPrepare(INSTANCE_ID, messageId, CREATION_TIME, 3)
    val promise = ConsensusPromise(INSTANCE_ID, messageId, CREATION_TIME, 3, true, 2)
    val propose = ConsensusPropose(INSTANCE_ID, messageId, CREATION_TIME, 3, true, emptyList())
    val accept = ConsensusAccept(INSTANCE_ID, messageId, CREATION_TIME, 3, true)

    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, prepare))
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, promise))
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, propose))
    messageHandler.handleMessage(messageSender, CONSENSUS_CHANNEL, getMsg(ORGANIZER_KEY, accept))

    // The handlers for prepare/promise/propose/accept should do nothing (call or update nothing)
    // because theses messages should only be handle in the backend server.
    Mockito.verify(mockLAORepository, Mockito.never()).getLaoByChannel(MockitoKotlinHelpers.any())
    Mockito.verify(mockConsensusRepo, Mockito.never())
      .updateNodesByChannel(MockitoKotlinHelpers.any())
  }

  companion object {
    private val ORGANIZER_KEY = Base64DataUtils.generateKeyPair()
    private val NODE_2_KEY = Base64DataUtils.generateKeyPair()
    private val NODE_3_KEY = Base64DataUtils.generateKeyPair()
    private val ORGANIZER = ORGANIZER_KEY.publicKey
    private val NODE_2 = NODE_2_KEY.publicKey
    private val NODE_3 = NODE_3_KEY.publicKey
    private val CREATION_TIME = Instant.now().epochSecond
    private const val LAO_NAME = "laoName"
    private val LAO_ID = generateLaoId(ORGANIZER, CREATION_TIME, LAO_NAME)
    private val CONSENSUS_CHANNEL = getLaoChannel(LAO_ID).subChannel("consensus")
    private const val TYPE = "election"
    private const val KEY_ID = "-t0xoQZa-ryiW18JnTjJHCsCNehFxuXOFOsfgKHHkj0="
    private const val PROPERTY = "state"
    private const val VALUE = "started"
    private val KEY = ConsensusKey(TYPE, KEY_ID, PROPERTY)
    private val INSTANCE_ID = generateConsensusId(TYPE, KEY_ID, PROPERTY)
    private val INVALID_MSG_ID = MessageID("SU5BVkxJRF9NU0c=")
    private val CREATE_LAO =
      CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, ORGANIZER, listOf(NODE_2, NODE_3))
    private val elect = ConsensusElect(CREATION_TIME, KEY_ID, TYPE, PROPERTY, VALUE)
  }
}
