package com.github.dedis.popstellar.ui.lao.event.election;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;
import timber.log.Timber;

@HiltViewModel
public class ElectionViewModel extends AndroidViewModel {
  public static final String TAG = ElectionViewModel.class.getSimpleName();

  private String laoId;

  private final LAORepository laoRepo;
  private final ElectionRepository electionRepo;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final RollCallRepository rollCallRepo;

  private final MutableLiveData<Boolean> isEncrypting = new MutableLiveData<>(false);

  @Inject
  public ElectionViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      ElectionRepository electionRepo,
      RollCallRepository rollCallRepo,
      GlobalNetworkManager networkManager,
      KeyManager keyManager) {
    super(application);
    this.laoRepo = laoRepo;
    this.electionRepo = electionRepo;
    this.rollCallRepo = rollCallRepo;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
  }

  /**
   * Creates new Election event.
   *
   * <p>Publish a GeneralMessage containing ElectionSetup data.
   *
   * @param electionVersion the version of the election
   * @param name the name of the election
   * @param creation the creation time of the election
   * @param start the start time of the election
   * @param end the end time of the election
   * @param questions questions of the election
   */
  public Completable createNewElection(
      ElectionVersion electionVersion,
      String name,
      long creation,
      long start,
      long end,
      List<ElectionQuestion.Question> questions) {
    Timber.tag(TAG).d("creating a new election with name %s", name);

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel();
    ElectionSetup electionSetup =
        new ElectionSetup(name, creation, start, end, laoView.getId(), electionVersion, questions);

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, electionSetup);
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public MutableLiveData<Boolean> getIsEncrypting() {
    return isEncrypting;
  }

  /**
   * Opens the election and publish opening message triggers ElectionOpen event on success or logs
   * appropriate error
   *
   * @param election election to be opened
   */
  public Completable openElection(Election election) {
    Timber.tag(TAG).d("opening election with name : %s", election.getName());
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = election.channel;
    String laoViewId = laoView.getId();

    // The time will have to be modified on the backend
    ElectionOpen electionOpen =
        new ElectionOpen(laoViewId, election.id, election.getStartTimestamp());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, electionOpen);
  }

  public Completable endElection(Election election) {
    Timber.tag(TAG).d("ending election with name : %s", election.getName());
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = election.channel;
    String laoViewId = laoView.getId();
    ElectionEnd electionEnd =
        new ElectionEnd(election.id, laoViewId, election.computeRegisteredVotesHash());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, electionEnd);
  }

  /**
   * Sends a ElectionCastVotes message .
   *
   * <p>Publish a GeneralMessage containing ElectionCastVotes data.
   *
   * @param votes the corresponding votes for that election
   */
  public Completable sendVote(String electionId, List<PlainVote> votes) {
    Election election;
    try {
      election = electionRepo.getElection(laoId, electionId);
    } catch (UnknownElectionException e) {
      Timber.tag(TAG).d("failed to retrieve current election");
      return Completable.error(e);
    }

    Timber.tag(TAG)
        .d(
            "sending a new vote in election : %s with election start time %d",
            election, election.getStartTimestamp());

    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(
            () -> keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId)))
        .doOnSuccess(token -> Timber.tag(TAG).d("Retrieved PoP Token to send votes : %s", token))
        .flatMapCompletable(
            token -> {
              CompletableFuture<CastVote> vote = createCastVote(votes, election, laoView);

              Channel electionChannel = election.channel;
              return networkManager.getMessageSender().publish(token, electionChannel, vote.get());
            });
  }

  /**
   * Function to enable the user to vote checking they have a valid pop token
   *
   * @return true if they can vote, false otherwise
   */
  public boolean canVote() {
    try {
      keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId));
    } catch (KeyException e) {
      return false;
    }
    return true;
  }

  @NonNull
  private CompletableFuture<CastVote> createCastVote(
      List<PlainVote> votes, Election election, LaoView laoView) {
    if (election.electionVersion == ElectionVersion.OPEN_BALLOT) {
      return CompletableFuture.completedFuture(
          new CastVote(votes, election.id, laoView.getId()));
    } else {
      isEncrypting.setValue(true);
      return CompletableFuture.supplyAsync(
          () -> {
            List<EncryptedVote> encryptedVotes = election.encrypt(votes);
            isEncrypting.postValue(false);
            new Handler(Looper.getMainLooper())
                .post(
                    () ->
                        Toast.makeText(getApplication(), R.string.vote_encrypted, Toast.LENGTH_LONG)
                            .show());
            return new CastVote(encryptedVotes, election.id, laoView.getId());
          },
          Executors.newSingleThreadExecutor());
    }
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
