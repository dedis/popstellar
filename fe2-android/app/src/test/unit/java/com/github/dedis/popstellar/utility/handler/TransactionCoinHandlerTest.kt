package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptInput
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptOutput
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.DataHandlingException
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class TransactionCoinHandlerTest {
  private lateinit var lao: Lao
  private lateinit var digitalCashRepo: DigitalCashRepository
  private lateinit var messageHandler: MessageHandler
  private lateinit var coinChannel: Channel
  private lateinit var gson: Gson
  private lateinit var postTransactionCoin: PostTransactionCoin

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var messageDao: MessageDao

  @Mock lateinit var transactionDao: TransactionDao

  @Mock lateinit var hashDao: HashDao

  @Mock lateinit var messageSender: MessageSender

  @Mock lateinit var keyManager: KeyManager

  @Before
  @Throws(
    GeneralSecurityException::class,
    DataHandlingException::class,
    IOException::class,
    UnknownRollCallException::class,
    UnknownLaoException::class,
    NoRollCallException::class
  )
  fun setup() {
    MockitoAnnotations.openMocks(this)
    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.lenient().`when`(keyManager.mainKeyPair).thenReturn(SENDER_KEY)
    Mockito.lenient().`when`(keyManager.mainPublicKey).thenReturn(SENDER)
    Mockito.lenient().`when`(messageSender.subscribe(MockitoKotlinHelpers.any())).then {
      Completable.complete()
    }

    Mockito.`when`(appDatabase.messageDao()).thenReturn(messageDao)
    Mockito.`when`(messageDao.takeFirstNMessages(ArgumentMatchers.anyInt()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(messageDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(messageDao.getMessageById(MockitoKotlinHelpers.any())).thenReturn(null)

    Mockito.`when`(appDatabase.transactionDao()).thenReturn(transactionDao)
    Mockito.`when`(transactionDao.getTransactionsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(transactionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(transactionDao.deleteByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())

    Mockito.`when`(appDatabase.hashDao()).thenReturn(hashDao)
    Mockito.`when`(hashDao.getDictionaryByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(ArrayList()))
    Mockito.`when`(hashDao.insertAll(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(hashDao.deleteByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())

    postTransactionCoin = PostTransactionCoin(TRANSACTION)
    digitalCashRepo = DigitalCashRepository(appDatabase, application)
    val messageRepo = MessageRepository(appDatabase, application)
    val dataRegistry = buildRegistry(digitalCashRepo, keyManager)
    gson = provideGson(dataRegistry)
    messageHandler = MessageHandler(messageRepo, dataRegistry)

    lao = Lao(CREATE_LAO.name, CREATE_LAO.organizer, CREATE_LAO.creation)
    lao.lastModified = lao.creation
    digitalCashRepo.initializeDigitalCash(lao.id, listOf(SENDER))
    coinChannel = lao.channel.subChannel("coin").subChannel(SENDER.encoded)
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
  fun testHandlePostTransactionCoin() {
    val message = MessageGeneral(SENDER_KEY, postTransactionCoin, gson)
    messageHandler.handleMessage(messageSender, coinChannel, message)
    val transactions = digitalCashRepo.getTransactions(lao.id, SENDER)

    Assert.assertNotNull(transactions)
    Assert.assertEquals(1, transactions!!.size.toLong())
    Assert.assertTrue(
      transactions.stream().anyMatch { transactionObject: TransactionObject ->
        transactionObject.channel == coinChannel
      }
    )
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val CREATE_LAO = CreateLao("lao", SENDER, ArrayList())

    // Version
    private const val VERSION = 1

    // Creation TxOut
    private const val TX_OUT_INDEX = 0
    private const val Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    private const val TYPE = "P2PKH"
    private val PUBKEY = SENDER.encoded
    private const val SIG = "CAFEBABE"
    private val SCRIPTTXIN = ScriptInput(TYPE, PublicKey(PUBKEY), Signature(SIG))
    private val TXIN = Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN)

    // Creation TXOUT
    private const val VALUE = 32
    private val PUBKEYHASH = SENDER.computeHash()
    private val SCRIPT_TX_OUT = ScriptOutput(TYPE, PUBKEYHASH)
    private val TXOUT = Output(VALUE.toLong(), SCRIPT_TX_OUT)

    // List TXIN, List TXOUT
    private val TX_INS = listOf(TXIN)
    private val TX_OUTS = listOf(TXOUT)

    // Locktime
    private const val TIMESTAMP: Long = 0

    // Transaction
    private val TRANSACTION = Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP)
  }
}
