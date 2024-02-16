package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpDao
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpEntity
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionDao
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SocialMediaDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var chirpDao: ChirpDao
  private lateinit var reactionDao: ReactionDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    chirpDao = appDatabase.chirpDao()
    reactionDao = appDatabase.reactionDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertChirpTest() {
    val testObserver = chirpDao.insert(CHIRP_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetChirpTest() {
    val testObserver = chirpDao.insert(CHIRP_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<Chirp>?> =
      chirpDao.getChirpsByLaoId(LAO_ID).test().assertValue { chirps: List<Chirp> ->
        chirps.size == 1 && chirps[0] == CHIRP_1
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  @Test
  fun insertReactionTest() {
    val testObserver = reactionDao.insert(REACTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetReactionTest() {
    val testObserver = reactionDao.insert(REACTION_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
    val testObserver2: TestObserver<List<Reaction>?> =
      reactionDao.getReactionsByChirpId(CHIRP1_ID).test().assertValue { reactions: List<Reaction> ->
        reactions.size == 1 && reactions[0] == REACTION_1
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  companion object {
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), 1000, "LAO")
    private val SENDER = Base64DataUtils.generatePublicKey()
    private val CHIRP1_ID = Base64DataUtils.generateMessageID()
    private val CHIRP2_ID = Base64DataUtils.generateMessageID()
    private const val EMOJI = "\uD83D\uDC4D"
    private val CHIRP_1 = Chirp(CHIRP1_ID, SENDER, "This is a chirp !", 1001, MessageID(""))
    private val CHIRP_2 = Chirp(CHIRP2_ID, SENDER, "This is another chirp !", 1003, MessageID(""))
    private val REACTION_1 =
      Reaction(
        Base64DataUtils.generateMessageID(),
        SENDER,
        EMOJI,
        CHIRP1_ID,
        Instant.now().epochSecond
      )
    private val CHIRP_ENTITY = ChirpEntity(LAO_ID, CHIRP_1)
    private val REACTION_ENTITY = ReactionEntity(REACTION_1)
  }
}
