package com.github.dedis.popstellar.ui.lao.witness;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class WitnessingViewModel extends AndroidViewModel implements QRCodeScanningViewModel {
  private static final Logger logger = LogManager.getLogger(WitnessingViewModel.class);

  private String laoId;

  // Scanned witnesses, not necessarily accepted by LAO
  private final Set<PublicKey> scannedWitnesses = new HashSet<>();

  // Accepted witnesses
  private final MutableLiveData<List<PublicKey>> witnesses = new MutableLiveData<>();
  private final MutableLiveData<List<WitnessMessage>> witnessMessages = new MutableLiveData<>();
  private final MutableLiveData<Integer> nbScanned = new MutableLiveData<>(0);

  private final LAORepository laoRepo;
  private final KeyManager keyManager;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public WitnessingViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      KeyManager keyManager,
      GlobalNetworkManager networkManager,
      Gson gson) {
    super(application);
    this.laoRepo = laoRepo;
    this.keyManager = keyManager;
    this.networkManager = networkManager;
    this.gson = gson;
  }

  public MutableLiveData<List<PublicKey>> getWitnesses() {
    return witnesses;
  }

  public MutableLiveData<List<WitnessMessage>> getWitnessMessages() {
    return witnessMessages;
  }

  public void setWitnessMessages(List<WitnessMessage> messages) {
    this.witnessMessages.setValue(messages);
  }

  public void setWitnesses(List<PublicKey> witnesses) {
    this.witnesses.setValue(witnesses);
  }

  /*
  // TODO refactor this away
   This is done so because of the absence of witnessing repository. It should be added
   and then witnesses can be observed the same way as events. You can have a look at
   either the roll call or election repository and view model.
  */
  public void setLaoId(String laoId) {
    this.laoId = laoId;

    disposables.add(
        laoRepo
            .getLaoObservable(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                laoView -> {
                  logger.debug("got an update for lao: " + laoView);

                  setWitnessMessages(new ArrayList<>(laoView.getWitnessMessages().values()));
                  setWitnesses(new ArrayList<>(laoView.getWitnesses()));
                },
                error -> logger.debug("error updating LAO :" + error)));
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.clear();
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    return nbScanned;
  }

  protected Completable signMessage(WitnessMessage witnessMessage) {
    logger.debug("signing message with ID " + witnessMessage.getMessageId());
    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), logger, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(keyManager::getMainKeyPair)
        .flatMapCompletable(
            keyPair -> {
              // Generate the signature of the message
              Signature signature = keyPair.sign(witnessMessage.getMessageId());

              logger.debug("Signed message id, resulting signature : " + signature);
              WitnessMessageSignature signatureMessage =
                  new WitnessMessageSignature(witnessMessage.getMessageId(), signature);

              return networkManager
                  .getMessageSender()
                  .publish(keyManager.getMainKeyPair(), laoView.getChannel(), signatureMessage);
            });
  }

  @Override
  public void handleData(String data) {
    MainPublicKeyData pkData;
    try {
      pkData = MainPublicKeyData.extractFrom(gson, data);
    } catch (Exception e) {
      ErrorUtils.logAndShow(
          getApplication().getApplicationContext(), logger, e, R.string.qr_code_not_main_pk);
      return;
    }
    PublicKey publicKey = pkData.getPublicKey();
    if (scannedWitnesses.contains(publicKey)) {
      ErrorUtils.logAndShow(getApplication(), logger, R.string.witness_already_scanned_warning);
      return;
    }

    scannedWitnesses.add(publicKey);
    nbScanned.setValue(scannedWitnesses.size());
    logger.debug("Witness " + publicKey + " successfully scanned");
    Toast.makeText(getApplication(), R.string.witness_scan_success, Toast.LENGTH_SHORT).show();
    disposables.add(
        updateLaoWitnesses()
            .subscribe(
                () -> {
                  String networkSuccess =
                      String.format(getApplication().getString(R.string.witness_added), publicKey);
                  logger.debug(networkSuccess);
                  Toast.makeText(getApplication(), networkSuccess, Toast.LENGTH_SHORT).show();
                },
                error -> {
                  scannedWitnesses.remove(publicKey);
                  nbScanned.setValue(scannedWitnesses.size());
                  ErrorUtils.logAndShow(getApplication(), logger, error, R.string.error_update_lao);
                }));
  }

  private Completable updateLaoWitnesses() {
    logger.debug("Updating lao witnesses");

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), logger, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel();
    KeyPair mainKey = keyManager.getMainKeyPair();
    long now = Instant.now().getEpochSecond();
    UpdateLao updateLao =
        new UpdateLao(
            mainKey.getPublicKey(),
            laoView.getCreation(),
            laoView.getName(),
            now,
            scannedWitnesses);
    MessageGeneral msg = new MessageGeneral(mainKey, updateLao, gson);

    return networkManager
        .getMessageSender()
        .publish(channel, msg)
        .doOnComplete(() -> logger.debug("updated lao witnesses"))
        .andThen(dispatchLaoUpdate(updateLao, laoView, channel, msg));
  }

  private Completable dispatchLaoUpdate(
      UpdateLao updateLao, LaoView laoView, Channel channel, MessageGeneral msg) {
    StateLao stateLao =
        new StateLao(
            updateLao.getId(),
            updateLao.getName(),
            laoView.getCreation(),
            updateLao.getLastModified(),
            laoView.getOrganizer(),
            msg.getMessageId(),
            updateLao.getWitnesses(),
            new ArrayList<>());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), channel, stateLao)
        .doOnComplete(() -> logger.debug("updated lao with " + stateLao));
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
