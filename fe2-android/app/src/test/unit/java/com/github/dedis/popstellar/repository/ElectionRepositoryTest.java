package com.github.dedis.popstellar.repository;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import org.junit.*;
import org.junit.runner.RunWith;

import java.util.Set;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static com.github.dedis.popstellar.testutils.ObservableUtils.assertCurrentValueIs;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public class ElectionRepositoryTest {

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 100321004, "Lao");
  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, 100321014, "Election")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();
  private static final Application APPLICATION = ApplicationProvider.getApplicationContext();
  private static AppDatabase appDatabase;
  private static ElectionRepository repo;

  @Before
  public void setup() {
    appDatabase = AppDatabaseModuleHelper.getAppDatabase(APPLICATION);
    repo = new ElectionRepository(appDatabase, APPLICATION);
  }

  @After
  public void tearDown() {
    appDatabase.close();
  }

  @Test
  public void addingElectionUpdatesIds() {
    TestObserver<Set<Election>> elections = repo.getElectionsObservableInLao(LAO_ID).test();

    assertCurrentValueIs(elections, emptySet());

    repo.updateElection(ELECTION);

    assertCurrentValueIs(elections, singleton(ELECTION));
  }

  @Test
  public void electionUpdateIsCorrectlyDispatched() throws UnknownElectionException {
    // Create repository with an election inside it
    repo.updateElection(ELECTION);
    TestObserver<Election> electionObserver =
        repo.getElectionObservable(LAO_ID, ELECTION.getId()).test();

    assertCurrentValueIs(electionObserver, ELECTION);
    assertThat(repo.getElection(LAO_ID, ELECTION.getId()), is(ELECTION));

    // Update Election
    Election updated = ELECTION.builder().setState(EventState.CLOSED).build();
    repo.updateElection(updated);

    // Assert that the update was correctly dispatched
    assertCurrentValueIs(electionObserver, updated);
    assertThat(repo.getElection(LAO_ID, ELECTION.getId()), is(updated));
  }

  @Test
  public void electionByChannelHasSameEffectAsGetElection() throws UnknownElectionException {
    repo.updateElection(ELECTION);

    assertThat(
        repo.getElection(LAO_ID, ELECTION.getId()),
        is(repo.getElectionByChannel(ELECTION.getChannel())));
  }

  @Test
  public void retrievingAnInvalidElectionThrowsAnException() {
    assertThrows(UnknownElectionException.class, () -> repo.getElection(LAO_ID, ELECTION.getId()));
    assertThrows(
        UnknownElectionException.class, () -> repo.getElectionObservable(LAO_ID, ELECTION.getId()));
  }
}
