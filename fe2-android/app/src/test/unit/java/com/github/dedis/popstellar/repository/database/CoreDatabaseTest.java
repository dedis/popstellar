package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.repository.database.core.CoreDao;
import com.github.dedis.popstellar.repository.database.core.CoreEntity;

import org.junit.*;
import org.junit.runner.RunWith;

import java.util.*;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class CoreDatabaseTest {
  private static AppDatabase appDatabase;
  private static CoreDao coreDao;

  private static final String[] SEED = new String[] {"foo", "bar", "foobar"};
  private static final String ADDRESS = "deadBeef";
  private static final Set<Channel> CHANNELS =
      new HashSet<>(Collections.singletonList(Channel.ROOT));

  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Before
  public void before() {
    hiltRule.inject();
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    coreDao = appDatabase.coreDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertTest() {
    CoreEntity coreEntity =
        new CoreEntity(0, ADDRESS, Collections.unmodifiableList(Arrays.asList(SEED)), CHANNELS);
    TestObserver<Void> testObserver = coreDao.insert(coreEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetTest() {
    CoreEntity coreEntity =
        new CoreEntity(0, ADDRESS, Collections.unmodifiableList(Arrays.asList(SEED)), CHANNELS);
    TestObserver<Void> testObserver = coreDao.insert(coreEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    CoreEntity get = coreDao.getSettings();

    assertEquals(ADDRESS, get.getServerAddress());
    assertArrayEquals(SEED, get.getWalletSeedArray());
    assertEquals(CHANNELS, get.getSubscriptions());
  }

  @Test
  public void insertAndReplaceTest() {
    CoreEntity coreEntity1 =
        new CoreEntity(0, ADDRESS, Collections.unmodifiableList(Arrays.asList(SEED)), CHANNELS);
    TestObserver<Void> testObserver = coreDao.insert(coreEntity1).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    String serverAddress = "address2";
    String[] seed = new String[] {"boo", "far", "r"};
    CoreEntity coreEntity2 =
        new CoreEntity(
            0, serverAddress, Collections.unmodifiableList(Arrays.asList(seed)), CHANNELS);
    TestObserver<Void> testObserver2 = coreDao.insert(coreEntity2).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    // Check that the entry has been replaced
    assertNotEquals(coreEntity1, coreDao.getSettings());
    assertEquals(coreEntity2, coreDao.getSettings());
  }
}
