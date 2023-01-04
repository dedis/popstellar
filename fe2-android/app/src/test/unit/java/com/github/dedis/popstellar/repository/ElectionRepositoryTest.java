package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import org.junit.Test;

import java.util.Set;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

public class ElectionRepositoryTest {

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 100321004, "Lao");
  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, 100321014, "Election")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();

  @Test
  public void addingElectionUpdatesIds() {
    ElectionRepository repo = new ElectionRepository();
    TestObserver<Set<String>> ids = repo.getElectionsObservable(LAO_ID).test();

    assertCurrentValueIs(ids, emptySet());

    repo.updateElection(ELECTION);

    assertCurrentValueIs(ids, singleton(ELECTION.getId()));
  }

  @Test
  public void electionUpdateIsCorrectlyDispatched() throws UnknownElectionException {
    // Create repository with an election inside it
    ElectionRepository repo = new ElectionRepository();
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
    ElectionRepository repo = new ElectionRepository();
    repo.updateElection(ELECTION);

    assertThat(
        repo.getElection(LAO_ID, ELECTION.getId()),
        is(repo.getElectionByChannel(ELECTION.getChannel())));
  }

  @Test
  public void retrievingAnInvalidElectionThrowsAnException() {
    ElectionRepository repo = new ElectionRepository();

    assertThrows(UnknownElectionException.class, () -> repo.getElection(LAO_ID, ELECTION.getId()));
    assertThrows(
        UnknownElectionException.class, () -> repo.getElectionObservable(LAO_ID, ELECTION.getId()));
  }

  private <T> void assertCurrentValueIs(TestObserver<T> observer, T expected) {
    observer.assertValueAt(observer.valueCount() - 1, expected);
  }
}
