package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElectionDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var electionDao: ElectionDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    electionDao = appDatabase.electionDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertTest() {
    val testObserver = electionDao.insert(ELECTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetTest() {
    val testObserver = electionDao.insert(ELECTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<Election>?> =
      electionDao.getElectionsByLaoId(LAO_ID).test().assertValue { elections: List<Election> ->
        elections.size == 1 && elections[0] == ELECTION
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  companion object {
    private val CREATION = Instant.now().epochSecond
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), CREATION, "Lao")
    private val ELECTION =
      ElectionBuilder(LAO_ID, CREATION + 10, "Election1")
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .build()
    private val ELECTION_ENTITY = ElectionEntity(ELECTION)
  }
}
