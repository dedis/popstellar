package com.github.dedis.popstellar.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.model.objects.InputObject
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObjectBuilder
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.ObservableUtils
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import io.reactivex.Completable
import io.reactivex.Single
import java.security.GeneralSecurityException
import java.util.stream.Collectors
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class DigitalCashRepositoryTest {
  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Mock private lateinit var appDatabase: AppDatabase

  @Mock private lateinit var transactionDao: TransactionDao

  @Mock private lateinit var hashDao: HashDao

  @JvmField @Rule(order = 0) val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Before
  fun initializeRepo() {
    Mockito.`when`(appDatabase.transactionDao()).thenReturn(transactionDao)
    Mockito.`when`(appDatabase.hashDao()).thenReturn(hashDao)
    repo = DigitalCashRepository(appDatabase, application)

    // Mock the DAOs
    Mockito.`when`(hashDao.getDictionaryByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(hashDao.deleteByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())
    Mockito.`when`(hashDao.insertAll(ArgumentMatchers.anyList())).thenReturn(Completable.complete())

    Mockito.`when`(transactionDao.getTransactionsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(transactionDao.deleteByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())
    Mockito.`when`(transactionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun updatingRepoWithoutInitializationThrowsError() {
    // In this test the actual content of the transaction do not matter
    val transactionObject = getValidTransactionBuilder("random id", USER1).build()
    Assert.assertThrows(NoRollCallException::class.java) {
      repo.updateTransactions(LAO_ID, transactionObject)
    }
  }

  @Test
  @Throws(GeneralSecurityException::class, NoRollCallException::class)
  fun transactionsTest() {
    // In this test the actual content of the transaction do not matter, only that it involves
    // user 1
    val transactionObject = getValidTransactionBuilder("random id", USER1).build()
    repo.initializeDigitalCash(LAO_ID, listOf(USER1_PK))
    repo.updateTransactions(LAO_ID, transactionObject)
    val transactionObjectList = repo.getTransactions(LAO_ID, USER1_PK)

    Assert.assertNotNull(transactionObjectList)
    Assert.assertEquals(1, transactionObjectList!!.size.toLong())
    Assert.assertEquals(transactionObject, transactionObjectList[0])
  }

  @Test
  @Throws(GeneralSecurityException::class, NoRollCallException::class)
  fun transactionsObservableTest() {
    // In this test the actual content of the transaction do not matter, only whether they concern
    // user 1 or user 2
    repo.initializeDigitalCash(LAO_ID, listOf(USER1_PK, USER2_PK))
    val transactionList = ArrayList<TransactionObject>()
    var observer = repo.getTransactionsObservable(LAO_ID, USER1_PK).test()

    // assert the observable is currently an empty list
    ObservableUtils.assertCurrentValueIs(observer, emptyList())
    val transactionObject = getValidTransactionBuilder("random id", USER1).build()
    transactionList.add(transactionObject)
    repo.updateTransactions(LAO_ID, transactionObject)

    // assert the observable is currently the list with the single transaction
    ObservableUtils.assertCurrentValueIs(observer, transactionList)
    val transactionObject2 = getValidTransactionBuilder("random id 2", USER1).build()
    transactionList.add(transactionObject2)
    repo.updateTransactions(LAO_ID, transactionObject2)

    // assert the observable is currently the list with the 2 elements
    ObservableUtils.assertCurrentValueIs(observer, transactionList)
    val transactionObjectAttendee = getValidTransactionBuilder("id3", USER2).build()
    repo.updateTransactions(LAO_ID, transactionObjectAttendee)
    observer = repo.getTransactionsObservable(LAO_ID, USER2_PK).test()

    // assert that the transaction from the new user is in the observable
    ObservableUtils.assertCurrentValueIs(observer, listOf(transactionObjectAttendee))
  }

  @Test
  @Throws(GeneralSecurityException::class, NoRollCallException::class)
  fun simpleIssuanceBalanceTest() {
    val issuanceAmount = 100000
    val issuance =
      getValidTransactionBuilder("any", ORGANIZER, listOf(USER1_PK, USER2_PK), true, issuanceAmount)
        .build()
    repo.initializeDigitalCash(LAO_ID, listOf(USER1_PK, USER2_PK))
    repo.updateTransactions(LAO_ID, issuance)

    Assert.assertEquals(issuanceAmount.toLong(), repo.getUserBalance(LAO_ID, USER1_PK))
    Assert.assertEquals(issuanceAmount.toLong(), repo.getUserBalance(LAO_ID, USER2_PK))

    val paymentAmount = 500
    val payment =
      getValidTransactionBuilder("anything", USER1, listOf(USER2_PK), false, paymentAmount).build()
    repo.updateTransactions(LAO_ID, payment)

    Assert.assertEquals(2, repo.getTransactions(LAO_ID, USER2_PK)!!.size.toLong())
    Assert.assertEquals(2, repo.getTransactions(LAO_ID, USER1_PK)!!.size.toLong())
    Assert.assertEquals(
      (issuanceAmount + paymentAmount).toLong(),
      repo.getUserBalance(LAO_ID, USER2_PK)
    )
    Assert.assertEquals(
      (issuanceAmount - paymentAmount).toLong(),
      repo.getUserBalance(LAO_ID, USER1_PK)
    )
  }

  companion object {
    private val ORGANIZER = Base64DataUtils.generateKeyPair()
    private val USER1 = Base64DataUtils.generateKeyPair()
    private val USER1_PK = USER1.publicKey
    private val USER2 = Base64DataUtils.generateKeyPair()
    private val USER2_PK = USER2.publicKey
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), 1000, "LAO")
    private const val DEFAULT_VALUE = Int.MAX_VALUE

    private lateinit var repo: DigitalCashRepository

    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun getValidTransactionBuilder(
      transactionId: String?,
      sender: KeyPair
    ): TransactionObjectBuilder {
      val builder = TransactionObjectBuilder()

      val senderPublicKey = sender.publicKey
      val type = "P2PKH"
      val txOutIndex = 0
      val txOutHash = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
      val scriptTxIn: ScriptInputObject
      val sig = sender.sign(senderPublicKey).encoded
      scriptTxIn = ScriptInputObject(type, senderPublicKey, Signature(sig))
      val input = InputObject(txOutHash, txOutIndex, scriptTxIn)
      val listInput = listOf(input)
      builder.setInputs(listInput)

      val channel = fromString("/root/laoId/coin/myChannel")
      builder.setChannel(channel)

      val pubKeyHash = senderPublicKey.computeHash()
      val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
      val output = OutputObject(DEFAULT_VALUE.toLong(), scriptTxOut)
      val listOutput = listOf(output)

      builder.setOutputs(listOutput)
      builder.setLockTime(0)
      builder.setVersion(0)
      builder.setTransactionId(transactionId)

      return builder
    }

    @Throws(GeneralSecurityException::class)
    fun getValidTransactionBuilder(
      transactionId: String?,
      sender: KeyPair,
      recipients: List<PublicKey>,
      isIssuance: Boolean,
      amount: Int
    ): TransactionObjectBuilder {
      val builder = TransactionObjectBuilder()

      val senderPublicKey = sender.publicKey
      val type = "P2PKH"
      val txOutIndex = 0
      val txOutHash =
        if (isIssuance) TransactionObject.TX_OUT_HASH_COINBASE
        else "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
      val scriptTxIn: ScriptInputObject
      val sig = sender.sign(senderPublicKey).encoded
      scriptTxIn = ScriptInputObject(type, senderPublicKey, Signature(sig))
      val input = InputObject(txOutHash, txOutIndex, scriptTxIn)
      val listInput = listOf(input)
      builder.setInputs(listInput)

      val channel = fromString("/root/laoId/coin/myChannel")
      builder.setChannel(channel)

      val outputObjects =
        recipients
          .stream()
          .map { recipient: PublicKey ->
            val pubKeyHash = recipient.computeHash()
            val scriptTxOut = ScriptOutputObject(type, pubKeyHash)
            OutputObject(amount.toLong(), scriptTxOut)
          }
          .collect(Collectors.toList())

      if (!isIssuance) {
        outputObjects.add(
          OutputObject(
            repo.getUserBalance(LAO_ID, senderPublicKey) - amount,
            ScriptOutputObject(type, senderPublicKey.computeHash())
          )
        )
      }

      builder.setOutputs(outputObjects)
      builder.setLockTime(0)
      builder.setVersion(0)
      builder.setTransactionId(transactionId)

      return builder
    }
  }
}
