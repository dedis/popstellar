package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao;
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;

@RunWith(AndroidJUnit4.class)
public class ElectionDatabaseTest {

  private static AppDatabase appDatabase;
  private static ElectionDao electionDao;

  private static final long CREATION = Instant.now().getEpochSecond();

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), CREATION, "Lao");
  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, CREATION + 10, "Election1")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();

  private static final Election ELECTION2 =
      new Election.ElectionBuilder(LAO_ID, CREATION + 20, "Election2")
          .setElectionVersion(ElectionVersion.SECRET_BALLOT)
          .build();

  private static final ElectionEntity ELECTION_ENTITY = new ElectionEntity(ELECTION);

  @Before
  public void before() {
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    electionDao = appDatabase.electionDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertTest() {
    TestObserver<Void> testObserver = electionDao.insert(ELECTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();
  }

  @Test
  public void insertAndGetTest() {
    TestObserver<Void> testObserver = electionDao.insert(ELECTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<String> emptyFilter = new HashSet<>();
    TestObserver<List<Election>> testObserver2 =
        electionDao
            .getElectionsByLaoId(LAO_ID, emptyFilter)
            .test()
            .assertValue(elections -> elections.size() == 1 && elections.get(0).equals(ELECTION));

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();
  }

  @Test
  public void getFilteredIdsTest() {
    TestObserver<Void> testObserver = electionDao.insert(ELECTION_ENTITY).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    Set<String> filter = new HashSet<>();
    filter.add(ELECTION.getId());
    TestObserver<List<Election>> testObserver2 =
        electionDao.getElectionsByLaoId(LAO_ID, filter).test().assertValue(List::isEmpty);

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    ElectionEntity newElectionEntity = new ElectionEntity(ELECTION2);
    TestObserver<Void> testObserver3 = electionDao.insert(newElectionEntity).test();

    testObserver3.awaitTerminalEvent();
    testObserver3.assertComplete();

    TestObserver<List<Election>> testObserver4 =
        electionDao
            .getElectionsByLaoId(LAO_ID, filter)
            .test()
            .assertValue(
                electionEntities ->
                    electionEntities.size() == 1 && electionEntities.get(0).equals(ELECTION2));

    testObserver4.awaitTerminalEvent();
    testObserver4.assertComplete();
  }
}