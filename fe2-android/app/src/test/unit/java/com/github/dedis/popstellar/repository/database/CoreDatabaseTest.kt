package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsEntity
import com.github.dedis.popstellar.repository.database.wallet.WalletDao
import com.github.dedis.popstellar.repository.database.wallet.WalletEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import java.time.Instant
import java.util.Collections
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreDatabaseTest {

  private lateinit var appDatabase: AppDatabase
  private lateinit var walletDao: WalletDao
  private lateinit var subscriptionsDao: SubscriptionsDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    walletDao = appDatabase.walletDao()
    subscriptionsDao = appDatabase.subscriptionsDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertWalletTest() {
    val walletEntity = WalletEntity(0, Collections.unmodifiableList(SEED.toList()))
    val testObserver = walletDao.insert(walletEntity).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetWalletTest() {
    val walletEntity = WalletEntity(0, Collections.unmodifiableList(SEED.toList()))
    val testObserver = walletDao.insert(walletEntity).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val get = walletDao.wallet
    Assert.assertNotNull(get)
    Assert.assertArrayEquals(SEED, get!!.walletSeedArray)
  }

  @Test
  fun insertAndReplaceWalletTest() {
    val walletEntity1 = WalletEntity(0, Collections.unmodifiableList(SEED.toList()))
    val testObserver = walletDao.insert(walletEntity1).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val seed = arrayOf("boo", "far", "r")
    val walletEntity2 = WalletEntity(0, Collections.unmodifiableList(seed.toList()))
    val testObserver2 = walletDao.insert(walletEntity2).test()
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()

    // Check that the entry has been replaced
    Assert.assertNotEquals(walletEntity1.getWalletSeed(), walletDao.wallet!!.getWalletSeed())
    Assert.assertEquals(walletEntity2.getWalletSeed(), walletDao.wallet!!.getWalletSeed())
  }

  @Test
  fun insertSubscriptionsTest() {
    val subscriptionsEntity = SubscriptionsEntity(LAO.id, ADDRESS, CHANNELS)
    val testObserver = subscriptionsDao.insert(subscriptionsEntity).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun subscriptionsByLaoTest() {
    val subscriptionsEntity = SubscriptionsEntity(LAO.id, ADDRESS, CHANNELS)
    val testObserver = subscriptionsDao.insert(subscriptionsEntity).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val subscriptionsEntity2 = subscriptionsDao.getSubscriptionsByLao(LAO.id)
    Assert.assertNotNull(subscriptionsEntity2)
    Assert.assertEquals(subscriptionsEntity.subscriptions, subscriptionsEntity2!!.subscriptions)
  }

  companion object {
    private const val LAO_NAME_1 = "LAO name 1"
    private val ORGANIZER = Base64DataUtils.generatePublicKey()
    private val LAO = Lao(LAO_NAME_1, ORGANIZER, Instant.now().epochSecond)
    private val SEED = arrayOf("foo", "bar", "foobar")
    private const val ADDRESS = "url"
    private val CHANNELS: Set<Channel> = HashSet(listOf(Channel.ROOT))
  }
}
