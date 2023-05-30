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
import java.util.HashSet;
import java.util.List;

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

    TestObserver<List<RollCall>> testObserver2 =
        rollCallDao
            .getRollCallsByLaoId(LAO_ID)
            .test()
            .assertValue(rollCalls -> rollCalls.size() == 1 && rollCalls.get(0).equals(ROLL_CALL));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }
}
