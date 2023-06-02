package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.witnessing.*;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class WitnessDatabaseTest {

  private static AppDatabase appDatabase;
  private static WitnessDao witnessDao;
  private static WitnessingDao witnessingDao;
  private static PendingDao pendingDao;

  private static final long CREATION = Instant.now().getEpochSecond();
  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), CREATION, "Lao");
  private static final PublicKey WITNESS = generateKeyPair().getPublicKey();
  private static final WitnessEntity WITNESS_ENTITY = new WitnessEntity(LAO_ID, WITNESS);
  private static final MessageID MESSAGE_ID = generateMessageID();
  private static final WitnessMessage WITNESS_MESSAGE = new WitnessMessage(MESSAGE_ID);
  private static final WitnessingEntity WITNESSING_ENTITY =
      new WitnessingEntity(LAO_ID, WITNESS_MESSAGE);
  private static final RollCall ROLL_CALL =
      new RollCall(
          LAO_ID,
          LAO_ID,
          "title",
          CREATION,
          CREATION + 10,
          CREATION + 20,
          EventState.CREATED,
          new HashSet<>(),
          "loc",
          "");
  private static final PendingEntity PENDING_ENTITY =
      new PendingEntity(MESSAGE_ID, LAO_ID, ROLL_CALL);

  @Before
  public void before() {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    witnessDao = appDatabase.witnessDao();
    witnessingDao = appDatabase.witnessingDao();
    pendingDao = appDatabase.pendingDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertWitnessTest() {
    List<WitnessEntity> witnesses = new ArrayList<>();
    witnesses.add(WITNESS_ENTITY);
    TestObserver<Void> testObserver = witnessDao.insertAll(witnesses).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void getWitnessTest() {
    List<WitnessEntity> witnesses = new ArrayList<>();
    witnesses.add(WITNESS_ENTITY);
    TestObserver<Void> testObserver = witnessDao.insertAll(witnesses).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<PublicKey>> testObserver2 =
        witnessDao
            .getWitnessesByLao(LAO_ID)
            .test()
            .assertValue(
                witnessList -> witnessList.size() == 1 && witnessList.get(0).equals(WITNESS));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void isWitnessTest() {
    assertEquals(0, witnessDao.isWitness(LAO_ID, WITNESS));

    List<WitnessEntity> witnesses = new ArrayList<>();
    witnesses.add(WITNESS_ENTITY);
    TestObserver<Void> testObserver = witnessDao.insertAll(witnesses).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    assertEquals(1, witnessDao.isWitness(LAO_ID, WITNESS));
  }

  @Test
  public void insertWitnessingTest() {
    TestObserver<Void> testObserver = witnessingDao.insert(WITNESSING_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void getWitnessingTest() {
    TestObserver<Void> testObserver = witnessingDao.insert(WITNESSING_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<WitnessMessage>> testObserver2 =
        witnessingDao
            .getWitnessMessagesByLao(LAO_ID)
            .test()
            .assertValue(
                witnessMessages ->
                    witnessMessages.size() == 1 && witnessMessages.get(0).equals(WITNESS_MESSAGE));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void deleteWitnessingTest() {
    TestObserver<Void> testObserver = witnessingDao.insert(WITNESSING_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<WitnessMessage>> testObserver2 =
        witnessingDao.getWitnessMessagesByLao(LAO_ID).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    Set<MessageID> filteredIds = new HashSet<>();
    filteredIds.add(MESSAGE_ID);
    TestObserver<Void> testObserver3 =
        witnessingDao.deleteMessagesByIds(LAO_ID, filteredIds).test();

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();

    TestObserver<List<WitnessMessage>> testObserver4 =
        witnessingDao.getWitnessMessagesByLao(LAO_ID).test().assertValue(List::isEmpty);

    testObserver4.awaitTerminalEvent();
    testObserver4.assertComplete();
  }

  @Test
  public void insertPendingTest() {
    TestObserver<Void> testObserver = pendingDao.insert(PENDING_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void getPendingTest() {
    TestObserver<Void> testObserver = pendingDao.insert(PENDING_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<PendingEntity>> testObserver2 =
        pendingDao
            .getPendingObjectsFromLao(LAO_ID)
            .test()
            .assertValue(
                pendingEntities ->
                    pendingEntities.size() == 1
                        && pendingEntities.get(0).getMessageID().equals(MESSAGE_ID)
                        && pendingEntities.get(0).getLaoId().equals(LAO_ID)
                        && pendingEntities.get(0).getObjectType().equals(Objects.ROLL_CALL)
                        && pendingEntities.get(0).getElection() == null
                        && pendingEntities.get(0).getMeeting() == null
                        && java.util.Objects.equals(
                            pendingEntities.get(0).getRollCall(), ROLL_CALL));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void removePendingTest() {
    TestObserver<Void> testObserver = pendingDao.insert(PENDING_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<List<PendingEntity>> testObserver2 =
        pendingDao.getPendingObjectsFromLao(LAO_ID).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    TestObserver<Void> testObserver3 = pendingDao.removePendingObject(MESSAGE_ID).test();

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();

    TestObserver<List<PendingEntity>> testObserver4 =
        pendingDao.getPendingObjectsFromLao(LAO_ID).test().assertValue(List::isEmpty);

    testObserver4.awaitTerminalEvent();
    testObserver4.assertComplete();
  }
}
