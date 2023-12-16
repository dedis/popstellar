package com.github.dedis.popstellar.repository;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static com.github.dedis.popstellar.testutils.ObservableUtils.assertCurrentValueIs;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ElectionRepositoryTest {

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 100321004, "Lao");
  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, 100321014, "Election")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();
  private static final Application APPLICATION = ApplicationProvider.getApplicationContext();
  @Mock private static AppDatabase appDatabase;
  @Mock private static ElectionDao electionDao;
  private static ElectionRepository repo;

  @Rule(order = 0)
  public final MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    when(appDatabase.electionDao()).thenReturn(electionDao);
    repo = new ElectionRepository(appDatabase, APPLICATION);

    when(electionDao.getElectionsByLaoId(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(electionDao.insert(any())).thenReturn(Completable.complete());
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
        repo.getElectionObservable(LAO_ID, ELECTION.id).test();

    assertCurrentValueIs(electionObserver, ELECTION);
    assertThat(repo.getElection(LAO_ID, ELECTION.id), is(ELECTION));

    // Update Election
    Election updated = ELECTION.builder().setState(EventState.CLOSED).build();
    repo.updateElection(updated);

    // Assert that the update was correctly dispatched
    assertCurrentValueIs(electionObserver, updated);
    assertThat(repo.getElection(LAO_ID, ELECTION.id), is(updated));
  }

  @Test
  public void electionByChannelHasSameEffectAsGetElection() throws UnknownElectionException {
    repo.updateElection(ELECTION);

    assertThat(
        repo.getElection(LAO_ID, ELECTION.id),
        is(repo.getElectionByChannel(ELECTION.channel)));
  }

  @Test
  public void retrievingAnInvalidElectionThrowsAnException() {
    assertThrows(UnknownElectionException.class, () -> repo.getElection(LAO_ID, ELECTION.id));
    assertThrows(
        UnknownElectionException.class, () -> repo.getElectionObservable(LAO_ID, ELECTION.id));
  }
}
