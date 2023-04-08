package com.github.dedis.popstellar.ui.lao.event.election;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.github.dedis.popstellar.utility.security.KeyManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;

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
    Log.d(TAG, "creating a new election with name " + name);

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
   * Opens the election and publish opening message triggers OpenElection event on success or logs
   * appropriate error
   *
   * @param election election to be opened
   */
  public Completable openElection(Election election) {
    Log.d(TAG, "opening election with name : " + election.getName());
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = election.getChannel();
    String laoViewId = laoView.getId();

    // The time will have to be modified on the backend
    OpenElection openElection =
        new OpenElection(laoViewId, election.getId(), election.getStartTimestamp());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, openElection);
  }

  public Completable endElection(Election election) {
    Log.d(TAG, "ending election with name : " + election.getName());
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = election.getChannel();
    String laoViewId = laoView.getId();
    ElectionEnd electionEnd =
        new ElectionEnd(election.getId(), laoViewId, election.computeRegisteredVotesHash());

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
      Log.d(TAG, "failed to retrieve current election");
      return Completable.error(e);
    }

    Log.d(
        TAG,
        "sending a new vote in election : "
            + election
            + " with election start time"
            + election.getStartTimestamp());

    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(
            () -> keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId)))
        .doOnSuccess(token -> Log.d(TAG, "Retrieved PoP Token to send votes : " + token))
        .flatMapCompletable(
            token -> {
              CastVote vote = createCastVote(votes, election, laoView);

              Channel electionChannel = election.getChannel();
              return networkManager.getMessageSender().publish(token, electionChannel, vote);
            });
  }

  @NonNull
  private CastVote createCastVote(List<PlainVote> votes, Election election, LaoView laoView) {
    if (election.getElectionVersion() == ElectionVersion.OPEN_BALLOT) {
      return new CastVote(votes, election.getId(), laoView.getId());
    } else {
      isEncrypting.postValue(true);
      CompletableFuture<CastVote> future =
          CompletableFuture.supplyAsync(
              () -> {
                List<EncryptedVote> encryptedVotes = election.encrypt(votes);
                isEncrypting.postValue(false);
                new Handler(Looper.getMainLooper())
                    .post(
                        () ->
                            Toast.makeText(getApplication(), "Vote encrypted !", Toast.LENGTH_LONG)
                                .show());
                return new CastVote(encryptedVotes, election.getId(), laoView.getId());
              },
              Executors.newSingleThreadExecutor());

      return future.join();
    }
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
