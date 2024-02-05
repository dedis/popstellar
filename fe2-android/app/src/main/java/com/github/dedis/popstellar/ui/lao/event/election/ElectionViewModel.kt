package com.github.dedis.popstellar.ui.lao.event.election

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionOpen
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class ElectionViewModel
@Inject
constructor(
    application: Application,
    private val laoRepo: LAORepository,
    private val electionRepo: ElectionRepository,
    private val rollCallRepo: RollCallRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager
) : AndroidViewModel(application) {
  private lateinit var laoId: String

  @JvmField val isEncrypting = MutableLiveData(false)

  /**
   * Creates new Election event.
   *
   * Publish a GeneralMessage containing ElectionSetup data.
   *
   * @param electionVersion the version of the election
   * @param name the name of the election
   * @param creation the creation time of the election
   * @param start the start time of the election
   * @param end the end time of the election
   * @param questions questions of the election
   */
  fun createNewElection(
      electionVersion: ElectionVersion,
      name: String,
      creation: Long,
      start: Long,
      end: Long,
      questions: List<Question>
  ): Completable {
    Timber.tag(TAG).d("creating a new election with name %s", name)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }
    val channel = laoView.channel
    val electionSetup =
        ElectionSetup(name, creation, start, end, laoView.id, electionVersion, questions)

    return networkManager.messageSender.publish(keyManager.mainKeyPair, channel, electionSetup)
  }

  fun setLaoId(laoId: String) {
    this.laoId = laoId
  }

  /**
   * Opens the election and publish opening message triggers ElectionOpen event on success or logs
   * appropriate error
   *
   * @param election election to be opened
   */
  fun openElection(election: Election): Completable {
    Timber.tag(TAG).d("opening election with name : %s", election.name)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    val channel = election.channel
    val laoViewId = laoView.id

    // The time will have to be modified on the backend
    val electionOpen = ElectionOpen(laoViewId, election.id, election.startTimestamp)
    return networkManager.messageSender.publish(keyManager.mainKeyPair, channel, electionOpen)
  }

  fun endElection(election: Election): Completable {
    Timber.tag(TAG).d("ending election with name : %s", election.name)
    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    val channel = election.channel
    val laoViewId = laoView.id
    val electionEnd = ElectionEnd(election.id, laoViewId, election.computeRegisteredVotesHash())
    return networkManager.messageSender.publish(keyManager.mainKeyPair, channel, electionEnd)
  }

  /**
   * Sends a ElectionCastVotes message .
   *
   * Publish a GeneralMessage containing ElectionCastVotes data.
   *
   * @param votes the corresponding votes for that election
   */
  fun sendVote(electionId: String, votes: List<PlainVote>): Completable {
    val election: Election =
        try {
          electionRepo.getElection(laoId, electionId)
        } catch (e: UnknownElectionException) {
          Timber.tag(TAG).e("failed to retrieve current election")
          return Completable.error(e)
        }

    Timber.tag(TAG)
        .d(
            "sending a new vote in election : %s with election start time %d",
            election,
            election.startTimestamp)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    return Single.fromCallable {
          keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId))
        }
        .doOnSuccess { token: PoPToken ->
          Timber.tag(TAG).d("Retrieved PoP Token to send votes : %s", token)
        }
        .flatMapCompletable { token: PoPToken ->
          val vote = createCastVote(votes, election, laoView)
          val electionChannel = election.channel
          networkManager.messageSender.publish(token, electionChannel, vote.get())
        }
  }

  /**
   * Function to enable the user to vote checking they have a valid pop token
   *
   * @return true if they can vote, false otherwise
   */
  fun canVote(): Boolean {
    try {
      keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId))
    } catch (e: KeyException) {
      Timber.tag(TAG).d(e)
      return false
    }
    return true
  }

  private fun createCastVote(
      votes: List<PlainVote>,
      election: Election,
      laoView: LaoView
  ): CompletableFuture<CastVote> {
    return if (election.electionVersion === ElectionVersion.OPEN_BALLOT) {
      CompletableFuture.completedFuture(CastVote(votes, election.id, laoView.id))
    } else {
      isEncrypting.value = true
      CompletableFuture.supplyAsync(
          {
            val encryptedVotes = election.encrypt(votes)
            isEncrypting.postValue(false)
            Handler(Looper.getMainLooper()).post {
              Toast.makeText(getApplication(), R.string.vote_encrypted, Toast.LENGTH_LONG).show()
            }
            CastVote(encryptedVotes, election.id, laoView.id)
          },
          Executors.newSingleThreadExecutor())
    }
  }

  @get:Throws(UnknownLaoException::class)
  private val lao: LaoView
    get() = laoRepo.getLaoView(laoId)

  companion object {
    val TAG: String = ElectionViewModel::class.java.simpleName
  }
}
