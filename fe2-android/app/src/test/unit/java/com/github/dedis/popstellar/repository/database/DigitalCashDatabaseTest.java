package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.repository.DigitalCashRepositoryTest;
import com.github.dedis.popstellar.repository.database.digitalcash.*;
import com.github.dedis.popstellar.testutils.Base64DataUtils;

import org.junit.*;
import org.junit.runner.RunWith;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;

@RunWith(AndroidJUnit4.class)
public class DigitalCashDatabaseTest {

  private static AppDatabase appDatabase;
  private static TransactionDao transactionDao;
  private static HashDao hashDao;

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 1000, "LAO1");
  private static final String LAO_ID2 = Lao.generateLaoId(generatePublicKey(), 1500, "LAO2");
  private static final KeyPair USER1 = Base64DataUtils.generateKeyPair();
  private static TransactionObject TRANSACTION_OBJECT;
  private static TransactionEntity TRANSACTION_ENTITY;
  private static HashEntity HASH_ENTITY =
      new HashEntity(USER1.getPublicKey().computeHash(), LAO_ID, USER1.getPublicKey());
  private static List<HashEntity> HASH_ENTITIES = new ArrayList<>();

  static {
    HASH_ENTITIES.add(HASH_ENTITY);
  }

  @Before
  public void before() throws GeneralSecurityException {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    transactionDao = appDatabase.transactionDao();
    hashDao = appDatabase.hashDao();
    TRANSACTION_OBJECT =
        DigitalCashRepositoryTest.getValidTransactionBuilder("random id", USER1).build();
    TRANSACTION_ENTITY = new TransactionEntity(LAO_ID, TRANSACTION_OBJECT);
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertTransactionTest() {
    TestObserver<Void> testObserver = transactionDao.insert(TRANSACTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void getTransactionsTest() {
    TestObserver<Void> testObserver = transactionDao.insert(TRANSACTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<TransactionObject>> testObserver2 =
        transactionDao
            .getTransactionsByLaoId(LAO_ID)
            .test()
            .assertValue(
                transactionObjects ->
                    transactionObjects.size() == 1
                        && transactionObjects.get(0).equals(TRANSACTION_OBJECT));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void deleteTransactionsByLaoTest() {
    TestObserver<Void> testObserver = transactionDao.insert(TRANSACTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<Void> testObserver2 = transactionDao.deleteByLaoId(LAO_ID).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    // Assert that the transaction is actually deleted
    TestObserver<List<TransactionObject>> testObserver3 =
        transactionDao.getTransactionsByLaoId(LAO_ID).test().assertValue(List::isEmpty);

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();
  }

  @Test
  public void insertHashTest() {
    TestObserver<Void> testObserver = hashDao.insert(HASH_ENTITIES).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void getHashTest() {
    TestObserver<Void> testObserver = hashDao.insert(HASH_ENTITIES).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<HashEntity>> testObserver2 =
        hashDao
            .getDictionaryByLaoId(LAO_ID)
            .test()
            .assertValue(
                transactionObjects ->
                    transactionObjects.size() == 1
                        && transactionObjects.get(0).equals(HASH_ENTITY));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void deleteHashByLaoTest() {
    TestObserver<Void> testObserver = hashDao.insert(HASH_ENTITIES).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<Void> testObserver2 = hashDao.deleteByLaoId(LAO_ID).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    // Assert that the transaction is actually deleted
    TestObserver<List<HashEntity>> testObserver3 =
        hashDao.getDictionaryByLaoId(LAO_ID).test().assertValue(List::isEmpty);

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();
  }
}
