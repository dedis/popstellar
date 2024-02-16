package com.github.dedis.popstellar.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.testutils.ObservableUtils
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import io.reactivex.Completable
import io.reactivex.Single
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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
class ElectionRepositoryTest {
  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Mock private lateinit var appDatabase: AppDatabase

  @Mock private lateinit var electionDao: ElectionDao
  private lateinit var repo: ElectionRepository

  @JvmField @Rule(order = 0) val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Before
  fun setup() {
    Mockito.`when`(appDatabase.electionDao()).thenReturn(electionDao)
    repo = ElectionRepository(appDatabase, application)

    Mockito.`when`(electionDao.getElectionsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(electionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
  }

  @Test
  fun addingElectionUpdatesIds() {
    val elections = repo.getElectionsObservableInLao(LAO_ID).test()
    ObservableUtils.assertCurrentValueIs(elections, emptySet())

    repo.updateElection(ELECTION)
    ObservableUtils.assertCurrentValueIs(elections, setOf(ELECTION))
  }

  @Test
  @Throws(UnknownElectionException::class)
  fun electionUpdateIsCorrectlyDispatched() {
    // Create repository with an election inside it
    repo.updateElection(ELECTION)
    val electionObserver = repo.getElectionObservable(LAO_ID, ELECTION.id).test()
    ObservableUtils.assertCurrentValueIs(electionObserver, ELECTION)
    MatcherAssert.assertThat(repo.getElection(LAO_ID, ELECTION.id), Matchers.`is`(ELECTION))

    // Update Election
    val updated = ELECTION.builder().setState(EventState.CLOSED).build()
    repo.updateElection(updated)

    // Assert that the update was correctly dispatched
    ObservableUtils.assertCurrentValueIs(electionObserver, updated)
    MatcherAssert.assertThat(repo.getElection(LAO_ID, ELECTION.id), Matchers.`is`(updated))
  }

  @Test
  @Throws(UnknownElectionException::class)
  fun electionByChannelHasSameEffectAsGetElection() {
    repo.updateElection(ELECTION)
    MatcherAssert.assertThat(
      repo.getElection(LAO_ID, ELECTION.id),
      Matchers.`is`(repo.getElectionByChannel(ELECTION.channel))
    )
  }

  @Test
  fun retrievingAnInvalidElectionThrowsAnException() {
    Assert.assertThrows(UnknownElectionException::class.java) {
      repo.getElection(LAO_ID, ELECTION.id)
    }
    Assert.assertThrows(UnknownElectionException::class.java) {
      repo.getElectionObservable(LAO_ID, ELECTION.id)
    }
  }

  companion object {
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), 100321004, "Lao")
    private val ELECTION =
      ElectionBuilder(LAO_ID, 100321014, "Election")
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .build()
  }
}
