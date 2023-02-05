package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.*;

@HiltViewModel
public class ConsensusViewModel extends AndroidViewModel {
  private static final String TAG = ConsensusViewModel.class.getSimpleName();

  private String laoId;

  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final LAORepository laoRepo;
  private final Gson gson;

  @Inject
  public ConsensusViewModel(
      @NonNull Application application,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      LAORepository laoRepo,
      Gson gson) {
    super(application);
    this.networkManager = networkManager;
    this.keyManager = keyManager;
    this.laoRepo = laoRepo;
    this.gson = gson;
  }

  /**
   * Sends a ConsensusElect message.
   *
   * <p>Publish a GeneralMessage containing ConsensusElect data.
   *
   * @param creation the creation time of the consensus
   * @param objId the id of the object the consensus refers to (e.g. election_id)
   * @param type the type of object the consensus refers to (e.g. election)
   * @param property the property the value refers to (e.g. "state")
   * @param value the proposed new value for the property (e.g. "started")
   * @return A single emitting the published message
   */
  public Single<MessageGeneral> sendConsensusElect(
      long creation, String objId, String type, String property, Object value) {
    Log.d(
        TAG,
        String.format(
            "creating a new consensus for type: %s, property: %s, value: %s",
            type, property, value));

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Single.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel().subChannel("consensus");
    ConsensusElect consensusElect = new ConsensusElect(creation, objId, type, property, value);

    MessageGeneral msg = new MessageGeneral(keyManager.getMainKeyPair(), consensusElect, gson);

    return networkManager.getMessageSender().publish(channel, msg).toSingleDefault(msg);
  }

  /**
   * Sends a ConsensusElectAccept message.
   *
   * <p>Publish a GeneralMessage containing ConsensusElectAccept data.
   *
   * @param electInstance the corresponding ElectInstance
   * @param accept true if accepted, false if rejected
   */
  public Completable sendConsensusElectAccept(ElectInstance electInstance, boolean accept) {
    MessageID messageId = electInstance.getMessageId();
    Log.d(
        TAG,
        "sending a new elect_accept for consensus with messageId : "
            + messageId
            + " with value "
            + accept);

    ConsensusElectAccept consensusElectAccept =
        new ConsensusElectAccept(electInstance.getInstanceId(), messageId, accept);

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), electInstance.getChannel(), consensusElectAccept);
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public Observable<List<ConsensusNode>> getNodes() throws UnknownLaoException {
    return laoRepo.getNodesByChannel(getLao().getChannel());
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
