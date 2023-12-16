package com.github.dedis.popstellar.repository;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpDao;
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionDao;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.*;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static com.github.dedis.popstellar.testutils.ObservableUtils.assertCurrentValueIs;
import static java.util.Collections.addAll;
import static java.util.Collections.emptySet;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class SocialMediaRepositoryTest {

  private static final Application APPLICATION = ApplicationProvider.getApplicationContext();
  @Mock private static AppDatabase appDatabase;
  @Mock private static ReactionDao reactionDao;
  @Mock private static ChirpDao chirpDao;
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
          generateMessageIDOtherThan(REACTION_1.id),
          generatePublicKey(),
          EMOJI,
          CHIRP1_ID,
          Instant.now().getEpochSecond());

  private static SocialMediaRepository repo;

  @Rule(order = 0)
  public final MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    when(appDatabase.chirpDao()).thenReturn(chirpDao);
    when(appDatabase.reactionDao()).thenReturn(reactionDao);
    repo = new SocialMediaRepository(appDatabase, APPLICATION);

    when(chirpDao.insert(any())).thenReturn(Completable.complete());
    when(reactionDao.insert(any())).thenReturn(Completable.complete());
    when(chirpDao.getChirpsByLaoId(anyString())).thenReturn(Single.just(Collections.emptyList()));
    when(reactionDao.getReactionsByChirpId(any())).thenReturn(Single.just(Collections.emptyList()));
  }

  @Test
  public void addingAChirpAfterSubscriptionUpdatesIds() {
    TestObserver<Set<MessageID>> ids = repo.getChirpsOfLao(LAO_ID).test();
    // assert the current element is an empty set
    assertCurrentValueIs(ids, emptySet());

    repo.addChirp(LAO_ID, CHIRP_1);

    // assert we received a new value : the set containing the chirp
    assertCurrentValueIs(ids, setOf(CHIRP_1.id));
  }

  @Test
  public void addingChirpBeforeSubscriptionUpdateIds() {
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Set<MessageID>> ids = repo.getChirpsOfLao(LAO_ID).test();

    // The value at subscription contains only the first chirp's id
    assertCurrentValueIs(ids, setOf(CHIRP_1.id));

    repo.addChirp(LAO_ID, CHIRP_2);

    // The value at subscription contains the two chirps' ids
    assertCurrentValueIs(ids, setOf(CHIRP_1.id, CHIRP_2.id));
  }

  @Test
  public void deleteChipDispatchToObservable() throws UnknownChirpException {
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.id).test();
    // Assert the value at start is the chirp
    assertCurrentValueIs(chirp, CHIRP_1);

    // Delete the chirp and make sure is was seen a present
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.id));

    // Assert a new value was dispatched and it is deleted
    assertCurrentValueIs(chirp, CHIRP_1.deleted());
  }

  @Test
  public void addChirpWithExistingIdHasNoEffect() throws UnknownChirpException {
    // Given a fresh repo, with an added chirp
    repo.addChirp(LAO_ID, CHIRP_1);

    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.id).test();
    assertCurrentValueIs(chirp, CHIRP_1);

    // Act
    Chirp invalidChirp =
        new Chirp(
            CHIRP_1.id,
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
    repo.addChirp(LAO_ID, CHIRP_1);

    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.id));
    TestObserver<Chirp> chirp = repo.getChirp(LAO_ID, CHIRP_1.id).test();
    // Retrieve current value count to make sure there are nothing more later
    int valueCount = chirp.valueCount();
    assertCurrentValueIs(chirp, CHIRP_1.deleted());

    // Assert a chirp was present
    assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.id));
    // But there is no new value published as the chirp was already deleted
    chirp.assertValueCount(valueCount);
  }

  @Test
  public void deletingANonExistingChirpReturnsFalse() {
    // Given a fresh repo, with an added chirp
    repo.addChirp(LAO_ID, CHIRP_1);

    assertFalse(repo.deleteChirp(LAO_ID, CHIRP_2.id));
  }

  @Test
  public void deletingAChirpDoesNotChangeTheIdSet() {
    assertFalse(repo.deleteChirp(LAO_ID, CHIRP_1.id));
  }

  @Test
  public void observingAnInvalidChirpThrowsAnError() {
    assertThrows(UnknownChirpException.class, () -> repo.getChirp(LAO_ID, CHIRP_1.id));
  }

  @Test
  public void addingValidReactionTest() {
    repo.addChirp(LAO_ID, CHIRP_1);

    assertTrue(repo.addReaction(LAO_ID, REACTION_1));
    assertTrue(repo.getReactionsByChirp(LAO_ID, CHIRP1_ID).contains(REACTION_1));
  }

  @Test
  public void addingReactionChangeSubjects() throws UnknownChirpException {
    repo.addChirp(LAO_ID, CHIRP_1);
    TestObserver<Set<Reaction>> reactions = repo.getReactions(LAO_ID, CHIRP1_ID).test();

    // assert the current element is an empty set
    assertCurrentValueIs(reactions, emptySet());

    repo.addReaction(LAO_ID, REACTION_1);

    // assert we received a new value : the set containing the chirp
    assertCurrentValueIs(reactions, setOf(REACTION_1));
  }

  @Test
  public void deletingReactionTest() {
    repo.addChirp(LAO_ID, CHIRP_1);
    repo.addReaction(LAO_ID, REACTION_1);
    Reaction deleted = REACTION_1.deleted();

    assertTrue(repo.deleteReaction(LAO_ID, REACTION_1.id));
    assertTrue(repo.getReactionsByChirp(LAO_ID, CHIRP1_ID).contains(deleted));
  }

  @Test
  public void addingReactionWithNoChirpTest() {
    repo.addReaction(LAO_ID, REACTION_1);
    assertFalse(repo.addReaction(LAO_ID, REACTION_1));
  }

  @Test
  public void deletingNonExistingReactionTest() {
    assertFalse(repo.deleteReaction(LAO_ID, REACTION_1.id));
  }

  @Test
  public void deletingADeletedReactionHasNoEffect() throws UnknownChirpException {
    repo.addChirp(LAO_ID, CHIRP_1);
    repo.addReaction(LAO_ID, REACTION_1);
    repo.addReaction(LAO_ID, REACTION_2);

    TestObserver<Set<Reaction>> reactions = repo.getReactions(LAO_ID, CHIRP_1.id).test();
    assertCurrentValueIs(reactions, setOf(REACTION_1, REACTION_2));

    assertTrue(repo.deleteReaction(LAO_ID, REACTION_1.id));
    assertTrue(repo.deleteReaction(LAO_ID, REACTION_1.id));
    assertCurrentValueIs(reactions, setOf(REACTION_2, REACTION_1.deleted()));
  }

  @SafeVarargs
  private static <E> Set<E> setOf(E... elems) {
    Set<E> set = new HashSet<>();
    addAll(set, elems);
    return set;
  }
}
