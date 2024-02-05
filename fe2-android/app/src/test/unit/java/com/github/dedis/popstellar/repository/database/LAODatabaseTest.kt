package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.lao.LAOEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LAODatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var laoDao: LAODao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    laoDao = appDatabase.laoDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertTest() {
    val laoEntity = LAOEntity(LAO.id, LAO)
    val testObserver = laoDao.insert(laoEntity).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetTest() {
    val laoEntity = LAOEntity(LAO.id, LAO)
    val testObserver = laoDao.insert(laoEntity).test()
    testObserver.awaitTerminalEvent()

    val testObserver2: TestObserver<List<Lao>?> =
      laoDao.allLaos.test().assertValue { laos: List<Lao> -> laos.size == 1 && laos[0] == LAO }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  companion object {
    private const val LAO_NAME_1 = "LAO name 1"
    private val ORGANIZER = Base64DataUtils.generatePublicKey()
    private val LAO = Lao(LAO_NAME_1, ORGANIZER, Instant.now().epochSecond)
  }
}
