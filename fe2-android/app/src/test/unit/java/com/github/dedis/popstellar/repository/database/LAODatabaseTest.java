package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;

@RunWith(AndroidJUnit4.class)
public class LAODatabaseTest {
  private static AppDatabase appDatabase;
  private static LAODao laoDao;

  private static final String LAO_NAME_1 = "LAO name 1";
  private static final PublicKey ORGANIZER = generatePublicKey();

  private static final Lao LAO = new Lao(LAO_NAME_1, ORGANIZER, Instant.now().getEpochSecond());

  @Before
  public void before() {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    laoDao = appDatabase.laoDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertTest() {
    LAOEntity laoEntity = new LAOEntity(LAO.getId(), LAO);
    TestObserver<Void> testObserver = laoDao.insert(laoEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetTest() {
    LAOEntity laoEntity = new LAOEntity(LAO.getId(), LAO);
    TestObserver<Void> testObserver = laoDao.insert(laoEntity).test();

    testObserver.awaitTerminalEvent();

    TestObserver<List<Lao>> testObserver2 =
        laoDao.getAllLaos().test().assertValue(laos -> laos.size() == 1 && laos.get(0).equals(LAO));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }
}
