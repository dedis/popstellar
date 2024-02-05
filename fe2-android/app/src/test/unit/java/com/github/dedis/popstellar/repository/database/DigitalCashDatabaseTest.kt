package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.repository.DigitalCashRepositoryTest.Companion.getValidTransactionBuilder
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao
import com.github.dedis.popstellar.repository.database.digitalcash.HashEntity
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.security.GeneralSecurityException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DigitalCashDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var transactionDao: TransactionDao
  private lateinit var hashDao: HashDao

  private lateinit var TRANSACTION_OBJECT: TransactionObject
  private lateinit var TRANSACTION_ENTITY: TransactionEntity

  @Before
  @Throws(GeneralSecurityException::class)
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    transactionDao = appDatabase.transactionDao()
    hashDao = appDatabase.hashDao()

    TRANSACTION_OBJECT = getValidTransactionBuilder("random id", USER1).build()
    TRANSACTION_ENTITY = TransactionEntity(LAO_ID, TRANSACTION_OBJECT)
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertTransactionTest() {
    val testObserver = transactionDao.insert(TRANSACTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun transactionsTest() {
    val testObserver = transactionDao.insert(TRANSACTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<TransactionObject>?> =
      transactionDao.getTransactionsByLaoId(LAO_ID).test().assertValue {
        transactionObjects: List<TransactionObject> ->
        (transactionObjects.size == 1 && transactionObjects[0] == TRANSACTION_OBJECT)
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  @Test
  fun deleteTransactionsByLaoTest() {
    val testObserver = transactionDao.insert(TRANSACTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2 = transactionDao.deleteByLaoId(LAO_ID).test()
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()

    // Assert that the transaction is actually deleted
    val testObserver3: TestObserver<List<TransactionObject>?> =
      transactionDao.getTransactionsByLaoId(LAO_ID).test().assertValue {
        obj: List<TransactionObject?> ->
        obj.isEmpty()
      }
    testObserver3.awaitTerminalEvent()
    testObserver3.assertComplete()
  }

  @Test
  fun insertHashTest() {
    val testObserver = hashDao.insertAll(HASH_ENTITIES).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun hashTest() {
    val testObserver = hashDao.insertAll(HASH_ENTITIES).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<HashEntity>?> =
      hashDao.getDictionaryByLaoId(LAO_ID).test().assertValue { transactionObjects: List<HashEntity>
        ->
        (transactionObjects.size == 1 && transactionObjects[0] == HASH_ENTITY)
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  @Test
  fun deleteHashByLaoTest() {
    val testObserver = hashDao.insertAll(HASH_ENTITIES).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2 = hashDao.deleteByLaoId(LAO_ID).test()
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()

    // Assert that the transaction is actually deleted
    val testObserver3: TestObserver<List<HashEntity>?> =
      hashDao.getDictionaryByLaoId(LAO_ID).test().assertValue { obj: List<HashEntity?> ->
        obj.isEmpty()
      }
    testObserver3.awaitTerminalEvent()
    testObserver3.assertComplete()
  }

  companion object {
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), 1000, "LAO1")
    private val LAO_ID2 = generateLaoId(Base64DataUtils.generatePublicKey(), 1500, "LAO2")
    private val USER1 = Base64DataUtils.generateKeyPair()
    private val HASH_ENTITY = HashEntity(USER1.publicKey.computeHash(), LAO_ID, USER1.publicKey)
    private val HASH_ENTITIES: List<HashEntity> = listOf(HASH_ENTITY)
  }
}
