package com.github.dedis.popstellar.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.socialmedia.ChirpDao
import com.github.dedis.popstellar.repository.database.socialmedia.ReactionDao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.ObservableUtils
import com.github.dedis.popstellar.utility.error.UnknownChirpException
import io.reactivex.Completable
import io.reactivex.Single
import java.time.Instant
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class SocialMediaRepositoryTest {
  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Mock private lateinit var appDatabase: AppDatabase

  @Mock private lateinit var reactionDao: ReactionDao

  @Mock private lateinit var chirpDao: ChirpDao

  private lateinit var repo: SocialMediaRepository

  @JvmField @Rule(order = 0) val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Before
  fun setup() {
    Mockito.`when`(appDatabase.chirpDao()).thenReturn(chirpDao)
    Mockito.`when`(appDatabase.reactionDao()).thenReturn(reactionDao)
    repo = SocialMediaRepository(appDatabase, application)

    Mockito.`when`(chirpDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(reactionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(chirpDao.getChirpsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(reactionDao.getReactionsByChirpId(MockitoKotlinHelpers.any()))
      .thenReturn(Single.just(emptyList()))
  }

  @Test
  fun addingAChirpAfterSubscriptionUpdatesIds() {
    val ids = repo.getChirpsOfLao(LAO_ID).test()
    // assert the current element is an empty set
    ObservableUtils.assertCurrentValueIs(ids, emptySet())
    repo.addChirp(LAO_ID, CHIRP_1)

    // assert we received a new value : the set containing the chirp
    ObservableUtils.assertCurrentValueIs(ids, setOf(CHIRP_1.id))
  }

  @Test
  fun addingChirpBeforeSubscriptionUpdateIds() {
    repo.addChirp(LAO_ID, CHIRP_1)
    val ids = repo.getChirpsOfLao(LAO_ID).test()

    // The value at subscription contains only the first chirp's id
    ObservableUtils.assertCurrentValueIs(ids, setOf(CHIRP_1.id))
    repo.addChirp(LAO_ID, CHIRP_2)

    // The value at subscription contains the two chirps' ids
    ObservableUtils.assertCurrentValueIs(ids, setOf(CHIRP_1.id, CHIRP_2.id))
  }

  @Test
  @Throws(UnknownChirpException::class)
  fun deleteChipDispatchToObservable() {
    repo.addChirp(LAO_ID, CHIRP_1)
    val chirp = repo.getChirp(LAO_ID, CHIRP_1.id).test()
    // Assert the value at start is the chirp
    ObservableUtils.assertCurrentValueIs(chirp, CHIRP_1)

    // Delete the chirp and make sure is was seen a present
    Assert.assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.id))

    // Assert a new value was dispatched and it is deleted
    ObservableUtils.assertCurrentValueIs(chirp, CHIRP_1.deleted())
  }

  @Test
  @Throws(UnknownChirpException::class)
  fun addChirpWithExistingIdHasNoEffect() {
    // Given a fresh repo, with an added chirp
    repo.addChirp(LAO_ID, CHIRP_1)
    val chirp = repo.getChirp(LAO_ID, CHIRP_1.id).test()
    ObservableUtils.assertCurrentValueIs(chirp, CHIRP_1)

    // Act
    val invalidChirp =
      Chirp(
        CHIRP_1.id,
        Base64DataUtils.generatePublicKey(),
        "This is another chirp !",
        1003,
        MessageID(""),
        LAO_ID
      )
    repo.addChirp(LAO_ID, invalidChirp)

    // Assert the current value is still the chirp
    ObservableUtils.assertCurrentValueIs(chirp, CHIRP_1)
  }

  @Test
  @Throws(UnknownChirpException::class)
  fun deletingADeletedChirpHasNoEffect() {
    repo.addChirp(LAO_ID, CHIRP_1)
    Assert.assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.id))
    val chirp = repo.getChirp(LAO_ID, CHIRP_1.id).test()
    // Retrieve current value count to make sure there are nothing more later
    val valueCount = chirp.valueCount()
    ObservableUtils.assertCurrentValueIs(chirp, CHIRP_1.deleted())

    // Assert a chirp was present
    Assert.assertTrue(repo.deleteChirp(LAO_ID, CHIRP_1.id))
    // But there is no new value published as the chirp was already deleted
    chirp.assertValueCount(valueCount)
  }

  @Test
  fun deletingANonExistingChirpReturnsFalse() {
    // Given a fresh repo, with an added chirp
    repo.addChirp(LAO_ID, CHIRP_1)
    Assert.assertFalse(repo.deleteChirp(LAO_ID, CHIRP_2.id))
  }

  @Test
  fun deletingAChirpDoesNotChangeTheIdSet() {
    Assert.assertFalse(repo.deleteChirp(LAO_ID, CHIRP_1.id))
  }

  @Test
  fun observingAnInvalidChirpThrowsAnError() {
    Assert.assertThrows(UnknownChirpException::class.java) { repo.getChirp(LAO_ID, CHIRP_1.id) }
  }

  @Test
  fun addingValidReactionTest() {
    repo.addChirp(LAO_ID, CHIRP_1)
    Assert.assertTrue(repo.addReaction(LAO_ID, REACTION_1))
    Assert.assertTrue(repo.getReactionsByChirp(LAO_ID, CHIRP1_ID).contains(REACTION_1))
  }

  @Test
  @Throws(UnknownChirpException::class)
  fun addingReactionChangeSubjects() {
    repo.addChirp(LAO_ID, CHIRP_1)
    val reactions = repo.getReactions(LAO_ID, CHIRP1_ID).test()

    // assert the current element is an empty set
    ObservableUtils.assertCurrentValueIs(reactions, emptySet())
    repo.addReaction(LAO_ID, REACTION_1)

    // assert we received a new value : the set containing the chirp
    ObservableUtils.assertCurrentValueIs(reactions, setOf(REACTION_1))
  }

  @Test
  fun deletingReactionTest() {
    repo.addChirp(LAO_ID, CHIRP_1)
    repo.addReaction(LAO_ID, REACTION_1)
    val deleted = REACTION_1.deleted()

    Assert.assertTrue(repo.deleteReaction(LAO_ID, REACTION_1.id))
    Assert.assertTrue(repo.getReactionsByChirp(LAO_ID, CHIRP1_ID).contains(deleted))
  }

  @Test
  fun addingReactionWithNoChirpTest() {
    repo.addReaction(LAO_ID, REACTION_1)
    Assert.assertFalse(repo.addReaction(LAO_ID, REACTION_1))
  }

  @Test
  fun deletingNonExistingReactionTest() {
    Assert.assertFalse(repo.deleteReaction(LAO_ID, REACTION_1.id))
  }

  @Test
  @Throws(UnknownChirpException::class)
  fun deletingADeletedReactionHasNoEffect() {
    repo.addChirp(LAO_ID, CHIRP_1)
    repo.addReaction(LAO_ID, REACTION_1)
    repo.addReaction(LAO_ID, REACTION_2)
    val reactions = repo.getReactions(LAO_ID, CHIRP_1.id).test()

    ObservableUtils.assertCurrentValueIs(reactions, setOf(REACTION_1, REACTION_2))
    Assert.assertTrue(repo.deleteReaction(LAO_ID, REACTION_1.id))
    Assert.assertTrue(repo.deleteReaction(LAO_ID, REACTION_1.id))
    ObservableUtils.assertCurrentValueIs(reactions, setOf(REACTION_2, REACTION_1.deleted()))
  }

  companion object {
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), 1000, "LAO")
    private val SENDER = Base64DataUtils.generatePublicKey()
    private val CHIRP1_ID = Base64DataUtils.generateMessageID()
    private val CHIRP2_ID = Base64DataUtils.generateMessageID()
    private const val EMOJI = "\uD83D\uDC4D"
    private val CHIRP_1 =
            Chirp(CHIRP1_ID, SENDER, "This is a chirp !", 1001, MessageID(""), LAO_ID)
    private val CHIRP_2 =
            Chirp(CHIRP2_ID, SENDER, "This is another chirp !", 1003, MessageID(""), LAO_ID)
    private val REACTION_1 =
      Reaction(
        Base64DataUtils.generateMessageID(),
        SENDER,
        EMOJI,
        CHIRP1_ID,
        Instant.now().epochSecond
      )
    private val REACTION_2 =
      Reaction(
        Base64DataUtils.generateMessageIDOtherThan(REACTION_1.id),
        Base64DataUtils.generatePublicKey(),
        EMOJI,
        CHIRP1_ID,
        Instant.now().epochSecond
      )

    @SafeVarargs
    private fun <E> setOf(vararg elems: E): Set<E> {
      val set: MutableSet<E> = HashSet()
      set.addAll(elems)
      return set
    }
  }
}
