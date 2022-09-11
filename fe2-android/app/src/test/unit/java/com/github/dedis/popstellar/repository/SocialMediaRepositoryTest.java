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
import static java.util.Collections.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    // make sure the current last element is an empty set
    ids.assertValueAt(ids.valueCount() - 1, emptySet());
    repo.addChirp(LAO_ID, CHIRP_1);
    // make sure we received a new value : the set containing the chirp
    ids.assertValueAt(ids.valueCount() - 1, singleton(CHIRP_1.getId()));
  }

  @Test
  public void addingChirpBeforeSubscriptionUpdateIds() {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Set<MessageID>> ids = repo.getChirpsOfLao(LAO_ID).test();
    // The value at subscription contains the first chirp's id
    ids.assertValueAt(ids.valueCount() - 1, singleton(CHIRP_1.getId()));
    repo.addChirp(LAO_ID, CHIRP_2);
    // The value at subscription contains the two chirps' ids
    ids.assertValueAt(ids.valueCount() - 1, setOf(CHIRP_1.getId(), CHIRP_2.getId()));
  }

  @Test
  public void deleteChipDispatchToObservable() throws UnknownChirpException {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.getId()).test();
    chirp.assertValueAt(chirp.valueCount() - 1, CHIRP_1);
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    chirp.assertValueAt(chirp.valueCount() - 1, CHIRP_1.deleted());
  }

  @Test
  public void addChirpWithExistingIdHasNoEffect() throws UnknownChirpException {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.getId()).test();
    chirp.assertValueAt(chirp.valueCount() - 1, CHIRP_1);
    Chirp invalidChirp =
        new Chirp(
            CHIRP_1.getId(),
            generatePublicKey(),
            "This is another chirp !",
            1003,
            new MessageID(""));
    repo.addChirp(LAO_ID, invalidChirp);
    chirp.assertValueAt(chirp.valueCount() - 1, CHIRP_1);
  }

  @Test
  public void deletingADeletedChirpHasNoEffect() throws UnknownChirpException {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.getId()).test();
    int valueCount = chirp.valueCount();
    chirp.assertValueAt(valueCount - 1, CHIRP_1.deleted());
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    // Assert there is no new value published
    chirp.assertValueCount(valueCount);
  }

  @Test
  public void deletingANonExistingChirpReturnsFalse() {
    SocialMediaRepository repo = new SocialMediaRepository();
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Set<MessageID>> ids = repo.getChirpsOfLao(LAO_ID).test();
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    int valueCount = ids.valueCount();
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
    // Assert there is no new value published
    ids.assertValueCount(valueCount);
  }

  @Test
  public void deletingAChirpDoesNotChangeTheIdSet() {
    SocialMediaRepository repo = new SocialMediaRepository();
    assertFalse(repo.deleteChirp(LAO_ID, CHIRP_1.getId()));
  }

  @SafeVarargs
  private static <E> Set<E> setOf(E... elems) {
    Set<E> set = new HashSet<>();
    addAll(set, elems);
    return set;
  }
}
