package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao;
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity;
import com.github.dedis.popstellar.repository.database.wallet.WalletDao;
import com.github.dedis.popstellar.repository.database.wallet.WalletEntity;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CoreDatabaseTest {
  private static AppDatabase appDatabase;
  private static WalletDao walletDao;
  private static SubscriptionsDao subscriptionsDao;

  private static final String LAO_NAME_1 = "LAO name 1";
  private static final PublicKey ORGANIZER = generatePublicKey();
  private static final Lao LAO = new Lao(LAO_NAME_1, ORGANIZER, Instant.now().getEpochSecond());

  private static final String[] SEED = new String[] {"foo", "bar", "foobar"};
  private static final String ADDRESS = "url";
  private static final Set<Channel> CHANNELS =
      new HashSet<>(Collections.singletonList(Channel.ROOT));

  @Before
  public void before() {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    walletDao = appDatabase.walletDao();
    subscriptionsDao = appDatabase.subscriptionsDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertWalletTest() {
    WalletEntity walletEntity =
        new WalletEntity(0, Collections.unmodifiableList(Arrays.asList(SEED)));
    TestObserver<Void> testObserver = walletDao.insert(walletEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetWalletTest() {
    WalletEntity walletEntity =
        new WalletEntity(0, Collections.unmodifiableList(Arrays.asList(SEED)));
    TestObserver<Void> testObserver = walletDao.insert(walletEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    WalletEntity get = walletDao.getWallet();

    assertArrayEquals(SEED, get.getWalletSeedArray());
  }

  @Test
  public void insertAndReplaceWalletTest() {
    WalletEntity walletEntity1 =
        new WalletEntity(0, Collections.unmodifiableList(Arrays.asList(SEED)));
    TestObserver<Void> testObserver = walletDao.insert(walletEntity1).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    String[] seed = new String[] {"boo", "far", "r"};
    WalletEntity walletEntity2 =
        new WalletEntity(0, Collections.unmodifiableList(Arrays.asList(seed)));
    TestObserver<Void> testObserver2 = walletDao.insert(walletEntity2).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    // Check that the entry has been replaced
    assertNotEquals(walletEntity1.getWalletSeed(), walletDao.getWallet().getWalletSeed());
    assertEquals(walletEntity2.getWalletSeed(), walletDao.getWallet().getWalletSeed());
  }

  @Test
  public void insertSubscriptionsTest() {
    SubscriptionsEntity subscriptionsEntity =
        new SubscriptionsEntity(LAO.getId(), ADDRESS, CHANNELS);
    TestObserver<Void> testObserver = subscriptionsDao.insert(subscriptionsEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void getSubscriptionsByLaoTest() {
    SubscriptionsEntity subscriptionsEntity =
        new SubscriptionsEntity(LAO.getId(), ADDRESS, CHANNELS);
    TestObserver<Void> testObserver = subscriptionsDao.insert(subscriptionsEntity).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    SubscriptionsEntity subscriptionsEntity2 = subscriptionsDao.getSubscriptionsByLao(LAO.getId());

    assertEquals(subscriptionsEntity.getSubscriptions(), subscriptionsEntity2.getSubscriptions());
  }
}
