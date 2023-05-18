package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.socialmedia.*;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;

@RunWith(AndroidJUnit4.class)
public class SocialMediaDatabaseTest {

  private static AppDatabase appDatabase;
  private static ChirpDao chirpDao;
  private static ReactionDao reactionDao;

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 1000, "LAO");

  private static final PublicKey SENDER = generatePublicKey();
  private static final MessageID CHIRP1_ID = generateMessageID();
  private static final MessageID CHIRP2_ID = generateMessageID();
  private static final String EMOJI = "\uD83D\uDC4D";
  private static final Chirp CHIRP_1 =
      new Chirp(CHIRP1_ID, SENDER, "This is a chirp !", 1001, new MessageID(""));
  private static final Chirp CHIRP_2 =
      new Chirp(CHIRP2_ID, SENDER, "This is another chirp !", 1003, new MessageID(""));

  private static final Reaction REACTION_1 =
      new Reaction(generateMessageID(), SENDER, EMOJI, CHIRP1_ID, Instant.now().getEpochSecond());

  private static final Reaction REACTION_2 =
      new Reaction(
          generateMessageIDOtherThan(REACTION_1.getId()),
          generatePublicKey(),
          EMOJI,
          CHIRP1_ID,
          Instant.now().getEpochSecond());

  private static final ChirpEntity CHIRP_ENTITY = new ChirpEntity(LAO_ID, CHIRP_1);
  private static final ReactionEntity REACTION_ENTITY = new ReactionEntity(REACTION_1);

  @Before
  public void before() {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    chirpDao = appDatabase.chirpDao();
    reactionDao = appDatabase.reactionDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertChirpTest() {
    TestObserver<Void> testObserver = chirpDao.insert(CHIRP_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetChirpTest() {
    TestObserver<Void> testObserver = chirpDao.insert(CHIRP_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<MessageID> emptyFilter = new HashSet<>();
    TestObserver<List<Chirp>> testObserver2 =
        chirpDao
            .getChirpsByLaoId(LAO_ID, emptyFilter)
            .test()
            .assertValue(chirps -> chirps.size() == 1 && chirps.get(0).equals(CHIRP_1));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void getChirpFilteredIdsTest() {
    TestObserver<Void> testObserver = chirpDao.insert(CHIRP_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<MessageID> filter = new HashSet<>();
    filter.add(CHIRP1_ID);
    TestObserver<List<Chirp>> testObserver2 =
        chirpDao.getChirpsByLaoId(LAO_ID, filter).test().assertValue(List::isEmpty);

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    ChirpEntity newChirpEntity = new ChirpEntity(LAO_ID, CHIRP_2);
    TestObserver<Void> testObserver3 = chirpDao.insert(newChirpEntity).test();

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();

    TestObserver<List<Chirp>> testObserver4 =
        chirpDao
            .getChirpsByLaoId(LAO_ID, filter)
            .test()
            .assertValue(chirps -> chirps.size() == 1 && chirps.get(0).equals(CHIRP_2));

    testObserver4.awaitTerminalEvent();
    testObserver4.assertComplete();
  }

  @Test
  public void insertReactionTest() {
    TestObserver<Void> testObserver = reactionDao.insert(REACTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetReactionTest() {
    TestObserver<Void> testObserver = reactionDao.insert(REACTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<MessageID> emptyFilter = new HashSet<>();
    TestObserver<List<Reaction>> testObserver2 =
        reactionDao
            .getReactionsByChirpId(CHIRP1_ID, emptyFilter)
            .test()
            .assertValue(reactions -> reactions.size() == 1 && reactions.get(0).equals(REACTION_1));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void getReactionFilteredIdsTest() {
    TestObserver<Void> testObserver = reactionDao.insert(REACTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<MessageID> filter = new HashSet<>();
    filter.add(REACTION_1.getId());
    TestObserver<List<Reaction>> testObserver2 =
        reactionDao.getReactionsByChirpId(CHIRP1_ID, filter).test().assertValue(List::isEmpty);

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    ReactionEntity newReactionEntity = new ReactionEntity(REACTION_2);
    TestObserver<Void> testObserver3 = reactionDao.insert(newReactionEntity).test();

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();

    TestObserver<List<Reaction>> testObserver4 =
        reactionDao
            .getReactionsByChirpId(CHIRP1_ID, filter)
            .test()
            .assertValue(reactions -> reactions.size() == 1 && reactions.get(0).equals(REACTION_2));

    testObserver4.awaitTerminalEvent();
    testObserver4.assertComplete();
  }
}
