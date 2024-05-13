package com.github.dedis.popstellar.ui.lao.socialmedia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.SocialMediaRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownChirpException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownReactionException
import com.github.dedis.popstellar.utility.error.keys.InvalidPoPTokenException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.util.Arrays
import java.util.stream.Collectors
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class SocialMediaViewModel
@Inject
constructor(
    application: Application,
    /*
     * Dependencies for this class
     */
    private val laoRepo: LAORepository,
    private val rollCallRepo: RollCallRepository,
    private val schedulerProvider: SchedulerProvider,
    private val socialMediaRepository: SocialMediaRepository,
    private val networkManager: GlobalNetworkManager,
    private val gson: Gson,
    private val keyManager: KeyManager
) : AndroidViewModel(application) {
  private lateinit var laoId: String

  /*
   * LiveData objects for capturing events
   */
  private val mNumberCharsLeft = MutableLiveData<Int>()
  val bottomNavigationTab = MutableLiveData(SocialMediaTab.HOME)

  private val _hasValidToken = MutableLiveData<Boolean>()

  private val disposables: CompositeDisposable = CompositeDisposable()

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  val numberCharsLeft: LiveData<Int>
    /*
     * Getters for MutableLiveData instances declared above
     */
    get() = mNumberCharsLeft

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  fun setNumberCharsLeft(numberChars: Int) {
    mNumberCharsLeft.value = numberChars
  }

  fun setBottomNavigationTab(tab: SocialMediaTab) {
    if (tab != bottomNavigationTab.value) {
      bottomNavigationTab.value = tab
    }
  }

  /**
   * Send a chirp to your own channel.
   *
   * Publish a MessageGeneral containing AddChirp data.
   *
   * @param text the text written in the chirp
   * @param parentId the id of the chirp to which you replied
   * @param timestamp the time at which you sent the chirp
   */
  fun sendChirp(text: String, parentId: MessageID?, timestamp: Long): Single<MessageGeneral> {
    Timber.tag(TAG).d("Sending a chirp")

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          Timber.tag(TAG).e(e, LAO_FAILURE_MESSAGE)
          return Single.error(UnknownLaoException())
        }

    val addChirp = AddChirp(text, parentId, timestamp)

    return Single.fromCallable { validPoPToken }
        .doOnSuccess { token: PoPToken ->
          Timber.tag(TAG).d("Retrieved PoPToken to send Chirp : %s", token)
        }
        .flatMap { token: PoPToken ->
          val channel = laoView.channel.subChannel(SOCIAL).subChannel(token.publicKey.encoded)
          val msg = MessageGeneral(token, addChirp, gson)
          networkManager.messageSender.publish(channel, msg).toSingleDefault(msg)
        }
  }

  /**
   * Send a reaction to the channel social/reactions
   *
   * Publish a MessageGeneral containing AddReaction data.
   *
   * @param codepoint unicode string of the reaction
   * @param chirpId chirp identifier to which the reaction is referred
   * @param timestamp the time at which the reaction has been generated
   */
  fun sendReaction(codepoint: String, chirpId: MessageID, timestamp: Long): Single<MessageGeneral> {
    Timber.tag(TAG).d("Sending a reaction to the chirp %s", chirpId)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          Timber.tag(TAG).e(e, LAO_FAILURE_MESSAGE)
          return Single.error(UnknownLaoException())
        }

    val addReaction = AddReaction(codepoint, chirpId, timestamp)

    return Single.fromCallable { validPoPToken }
        .doOnSuccess { token: PoPToken ->
          Timber.tag(TAG).d("Retrieved PoPToken to send Reaction : %s", token)
        }
        .flatMap { token: PoPToken ->
          val channel = laoView.channel.subChannel(SOCIAL).subChannel(REACTIONS)
          val msg = MessageGeneral(token, addReaction, gson)
          networkManager.messageSender.publish(channel, msg).toSingleDefault(msg)
        }
  }

  fun deleteChirp(chirpId: MessageID, timestamp: Long): Single<MessageGeneral> {
    Timber.tag(TAG).d("Deleting the chirp with id: %s", chirpId)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          Timber.tag(TAG).e(e, LAO_FAILURE_MESSAGE)
          return Single.error(UnknownLaoException())
        }
    val deleteChirp = DeleteChirp(chirpId, timestamp)
    return Single.fromCallable { validPoPToken }
        .doOnSuccess { token: PoPToken? ->
          Timber.tag(TAG).d("Retrieved PoPToken to delete Chirp : %s", token)
        }
        .flatMap { token: PoPToken ->
          val channel = laoView.channel.subChannel(SOCIAL).subChannel(token.publicKey.encoded)
          val msg = MessageGeneral(token, deleteChirp, gson)
          networkManager.messageSender.publish(channel, msg).toSingleDefault(msg)
        }
  }

  /**
   * Delete a reaction through the channel social/reactions
   *
   * Publish a MessageGeneral containing DeleteReaction data.
   *
   * @param chirpId chirp identifier to which the reaction is referred
   * @param timestamp the time at which the reaction has been deleted
   * @param emoji unicode string of the reaction to delete
   */
  fun deleteReaction(chirpId: MessageID, timestamp: Long, emoji: String): Single<MessageGeneral> {
    Timber.tag(TAG).d("Deleting reaction %s of chirp %s", emoji, chirpId)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          Timber.tag(TAG).e(e, LAO_FAILURE_MESSAGE)
          return Single.error(UnknownLaoException())
        }

    return Single.fromCallable { validPoPToken }
        .doOnSuccess { token: PoPToken? ->
          Timber.tag(TAG).d("Retrieved PoPToken to delete Reaction : %s", token)
        }
        .flatMap { token: PoPToken ->
          val channel = laoView.channel.subChannel(SOCIAL).subChannel(REACTIONS)

          // Find the reaction id (alive reaction sent from myself matching the emoji)
          val reactions: Set<Reaction> = socialMediaRepository.getReactionsByChirp(laoId, chirpId)
          val previousReaction =
              reactions
                  .stream()
                  .filter { reaction: Reaction ->
                    !reaction.isDeleted &&
                        reaction.codepoint == emoji &&
                        reaction.sender == token.publicKey
                  }
                  .findFirst()
                  .orElse(null) ?: throw UnknownReactionException()

          val deleteReaction = DeleteReaction(previousReaction.id, timestamp)
          val msg = MessageGeneral(token, deleteReaction, gson)

          networkManager.messageSender.publish(channel, msg).toSingleDefault(msg)
        }
  }

  val chirps: Observable<List<Chirp>>
    get() =
        socialMediaRepository
            .getChirpsOfLao(laoId)
            // Retrieve chirp subjects per id
            .map { ids: Set<MessageID> ->
              val chirps: MutableList<Observable<Chirp>> = ArrayList(ids.size)
              for (id in ids) {
                chirps.add(socialMediaRepository.getChirp(laoId, id))
              }
              chirps
            }
            // Zip the subjects together to a sorted list
            .flatMap { observables: List<Observable<Chirp>> ->
              Observable.combineLatest(observables) { chirps: Array<Any?> ->
                Arrays.stream(chirps)
                    .map { obj: Any? -> Chirp::class.java.cast(obj) }
                    .sorted(
                        Comparator.comparing { chirp: Chirp? ->
                          if (chirp != null) -chirp.timestamp else 0
                        })
                    .collect(Collectors.toList())
              }
            }
            // We want to observe these changes on the main thread such that any modification done
            // to
            // the view are done on the thread. Otherwise, the app might crash
            .observeOn(schedulerProvider.mainThread())

  @Throws(UnknownChirpException::class)
  fun getReactions(chirpId: MessageID): Observable<Set<Reaction>> {
    return socialMediaRepository
        .getReactions(laoId, chirpId)
        .observeOn(schedulerProvider.mainThread())
  }

  /**
   * Check whether the sender of a chirp is the current user
   *
   * @param sender String of the PoPToken PublicKey
   * @return true if the sender is the current user
   */
  fun isOwner(sender: String): Boolean {
    Timber.tag(TAG).d("Testing if the sender is also the owner")

    return try {
      val token = validPoPToken

      sender == token.publicKey.encoded
    } catch (e: KeyException) {
      if (_hasValidToken.value == true) {
        logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token)
      }
      false
    }
  }

  /**
   * This function searches if in the list of the senders of a reaction, there's the own key
   *
   * @param senders set of public keys as encoded strings
   * @return true if we have sent such reaction, false otherwise
   */
  fun isReactionPresent(senders: Set<String?>): Boolean {
    return try {
      val token = validPoPToken
      val toSearch = token.publicKey.encoded
      senders.contains(toSearch)
    } catch (e: KeyException) {
      if (_hasValidToken.value == true) {
        logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token)
      }
      false
    }
  }

  fun setLaoId(laoId: String) {
    this.laoId = laoId
  }

  @get:Throws(KeyException::class)
  val validPoPToken: PoPToken
    get() = keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId))

  @get:Throws(UnknownLaoException::class)
  private val lao: LaoView
    get() = laoRepo.getLaoView(laoId)

  fun checkValidPoPToken() {
    disposables.add(
        Single.fromCallable {
              keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
            .subscribe(
                { _ -> _hasValidToken.postValue(true) },
                { error ->
                  if (error is InvalidPoPTokenException) {
                    _hasValidToken.postValue(false)
                  } else {
                    Timber.tag(TAG).e(error, "Error checking PoPToken validity")
                  }
                }))
  }

  companion object {
    // TODO : looks like those constants need to be moved to resources
    val TAG: String = SocialMediaViewModel::class.java.simpleName
    private const val LAO_FAILURE_MESSAGE = "failed to retrieve lao"
    private const val SOCIAL = "social"
    private const val REACTIONS = "reactions"
    const val MAX_CHAR_NUMBERS = 300
    const val MAX_CHAR_NUMBERS_STR = "300"
  }
}
