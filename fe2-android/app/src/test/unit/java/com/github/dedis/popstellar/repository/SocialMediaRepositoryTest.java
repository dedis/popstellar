package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static java.util.Collections.addAll;
import static java.util.Collections.emptySet;
import static org.junit.Assert.*;

public class SocialMediaRepositoryTest {

  private static final String LAO_ID = Lao.generateLaoId(generatePublicKey(), 1000, "LAO");

  private static final Chirp CHIRP_1 =
      new Chirp(
          generateMessageID(), generatePublicKey(), "This is a chirp !", 1001, new MessageID(""));
  private static final Chirp CHIRP_2 =
      new Chirp(
          generateMessageID(),
          generatePublicKey(),
          "This is another chirp !",
          1003,
          new MessageID(""));

  @Test
  public void addingAChirpAfterSubscriptionUpdatesIds() {
    SocialMediaRepository repo = new SocialMediaRepository();
    TestObserver<Set<MessageID>> ids = repo.getChirpsOfLao(LAO_ID).test();
    // assert the current element is an empty set
    assertCurrentValueIs(ids, emptySet());

    repo.addChirp(LAO_ID, CHIRP_1);

    // assert we received a new value : the set containing the chirp
    assertCurrentValueIs(ids, setOf(CHIRP_1.getId()));
  }

  @Test
  public void addingChirpBeforeSubscriptionUpdateIds() {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Set<MessageID>> ids = repo.getChirpsOfLao(LAO_ID).test();

    // The value at subscription contains only the first chirp's id
    assertCurrentValueIs(ids, setOf(CHIRP_1.getId()));

    repo.addChirp(LAO_ID, CHIRP_2);

    // The value at subscription contains the two chirps' ids
    assertCurrentValueIs(ids, setOf(CHIRP_1.getId(), CHIRP_2.getId()));
  }

  @Test
  public void deleteChipDispatchToObservable() throws UnknownChirpException {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.getId()).test();
    // Assert the value at start is the chirp
    assertCurrentValueIs(chirp, CHIRP_1);

    // Delete the chirp and make sure is was seen a present
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));

    // Assert a new value was dispatched and it is deleted
    assertCurrentValueIs(chirp, CHIRP_1.deleted());
  }

  @Test
  public void addChirpWithExistingIdHasNoEffect() throws UnknownChirpException {
    // Given a fresh repo, with an added chirp
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);

    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.getId()).test();
    assertCurrentValueIs(chirp, CHIRP_1);

    // Act
    Chirp invalidChirp =
        new Chirp(
            CHIRP_1.getId(),
            generatePublicKey(),
            "This is another chirp !",
            1003,
            new MessageID(""));
    repo.addChirp(LAO_ID, invalidChirp);

    // Assert the current value is still the chirp
    assertCurrentValueIs(chirp, CHIRP_1);
  }

  @Test
  public void deletingADeletedChirpHasNoEffect() throws UnknownChirpException {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);

    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.getId()).test();
    // Retrieve current value count to make sure there are nothing more later
    int valueCount = chirp.valueCount();
    assertCurrentValueIs(chirp, CHIRP_1.deleted());

    // Assert a chirp was present
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    // But there is no new value published as the chirp was already deleted
    chirp.assertValueCount(valueCount);
  }

  @Test
  public void deletingANonExistingChirpReturnsFalse() {
    // Given a fresh repo, with an added chirp
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);

    assertFalse(repo.deleteChirp(LAO_ID, CHIRP_2.getId()));
  }

  @Test
  public void deletingAChirpDoesNotChangeTheIdSet() {
    SocialMediaRepository repo = new SocialMediaRepository();
    assertFalse(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
  }

  @Test
  public void observingAnInvalidChirpThrowsAnError() {
    SocialMediaRepository repo = new SocialMediaRepository();
    assertThrows(UnknownChirpException.class, () -> repo.getChirp(LAO_ID, CHIRP_1.getId()));
  }

  @SafeVarargs
  private static <E> Set<E> setOf(E... elems) {
    Set<E> set = new HashSet<>();
    addAll(set, elems);
    return set;
  }

  private <T> void assertCurrentValueIs(TestObserver<T> ids, T value) {
    ids.assertValueAt(ids.valueCount() - 1, value);
  }
}
