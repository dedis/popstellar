package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;

@RunWith(AndroidJUnit4.class)
public class RollCallDatabaseTest {

  private static AppDatabase appDatabase;
  private static RollCallDao rollCallDao;

  private static final long CREATION = Instant.now().getEpochSecond();

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), CREATION, "Lao");
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
  private static final RollCall ROLL_CALL2 =
      new RollCall(
          LAO_ID + "2",
          LAO_ID + "2",
          "title2",
          CREATION,
          CREATION + 10,
          CREATION + 20,
          EventState.CREATED,
          new HashSet<>(),
          "loc2",
          "description");

  private static final RollCallEntity ROLLCALL_ENTITY = new RollCallEntity(LAO_ID, ROLL_CALL);

  @Before
  public void before() {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    rollCallDao = appDatabase.rollCallDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertTest() {
    TestObserver<Void> testObserver = rollCallDao.insert(ROLLCALL_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetTest() {
    TestObserver<Void> testObserver = rollCallDao.insert(ROLLCALL_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<String> emptyFilter = new HashSet<>();
    TestObserver<List<RollCall>> testObserver2 =
        rollCallDao
            .getRollCallsByLaoId(LAO_ID, emptyFilter)
            .test()
            .assertValue(rollCalls -> rollCalls.size() == 1 && rollCalls.get(0).equals(ROLL_CALL));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void getFilteredIdsTest() {
    TestObserver<Void> testObserver = rollCallDao.insert(ROLLCALL_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<String> filter = new HashSet<>();
    filter.add(ROLL_CALL.getPersistentId());
    TestObserver<List<RollCall>> testObserver2 =
        rollCallDao.getRollCallsByLaoId(LAO_ID, filter).test().assertValue(List::isEmpty);

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    RollCallEntity newRollCallEntity = new RollCallEntity(LAO_ID, ROLL_CALL2);
    TestObserver<Void> testObserver3 = rollCallDao.insert(newRollCallEntity).test();

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();

    TestObserver<List<RollCall>> testObserver4 =
        rollCallDao
            .getRollCallsByLaoId(LAO_ID, filter)
            .test()
            .assertValue(rollCalls -> rollCalls.size() == 1 && rollCalls.get(0).equals(ROLL_CALL2));

    testObserver4.awaitTerminalEvent();
    testObserver4.assertComplete();
  }
}
