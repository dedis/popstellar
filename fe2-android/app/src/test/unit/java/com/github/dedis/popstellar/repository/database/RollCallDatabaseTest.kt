package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RollCallDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var rollCallDao: RollCallDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    rollCallDao = appDatabase.rollCallDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertTest() {
    val testObserver = rollCallDao.insert(ROLLCALL_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetTest() {
    val testObserver = rollCallDao.insert(ROLLCALL_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<RollCall>?> =
      rollCallDao.getRollCallsByLaoId(LAO_ID).test().assertValue { rollCalls: List<RollCall> ->
        rollCalls.size == 1 && rollCalls[0] == ROLL_CALL
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  companion object {
    private val CREATION = Instant.now().epochSecond
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), CREATION, "Lao")
    private val ROLL_CALL =
      RollCall(
        LAO_ID,
        LAO_ID,
        "title",
        CREATION,
        CREATION + 10,
        CREATION + 20,
        EventState.CREATED,
        HashSet(),
        "loc",
        ""
      )
    private val ROLLCALL_ENTITY = RollCallEntity(LAO_ID, ROLL_CALL)
  }
}
