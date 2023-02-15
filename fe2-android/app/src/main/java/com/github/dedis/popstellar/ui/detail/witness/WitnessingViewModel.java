package com.github.dedis.popstellar.ui.detail.witness;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

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

import java.time.Instant;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class WitnessingViewModel extends AndroidViewModel implements QRCodeScanningViewModel {
  public static final String TAG = WitnessingViewModel.class.getSimpleName();

  private String laoId;
  private final Set<PublicKey> witnesses = new HashSet<>();

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

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public Completable signMessage(WitnessMessage witnessMessage) {
    Log.d(TAG, "signing message with ID " + witnessMessage.getMessageId());
    final LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    return Single.fromCallable(keyManager::getMainKeyPair)
        .flatMapCompletable(
            keyPair -> {
              // Generate the signature of the message
              Signature signature = keyPair.sign(witnessMessage.getMessageId());

              Log.d(TAG, "Signed message id, resulting signature : " + signature);
              WitnessMessageSignature signatureMessage =
                  new WitnessMessageSignature(witnessMessage.getMessageId(), signature);

              return networkManager
                  .getMessageSender()
                  .publish(keyManager.getMainKeyPair(), laoView.getChannel(), signatureMessage);
            });
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }

  @Override
  public void handleData(String data) {
    MainPublicKeyData pkData;
    try {
      pkData = MainPublicKeyData.extractFrom(gson, data);
    } catch (Exception e) {
      ErrorUtils.logAndShow(
          getApplication().getApplicationContext(), TAG, e, R.string.qr_code_not_main_pk);
      return;
    }
    PublicKey publicKey = pkData.getPublicKey();
    if (witnesses.contains(publicKey)) {
      ErrorUtils.logAndShow(getApplication(), TAG, R.string.witness_already_scanned_warning);
      return;
    }

    witnesses.add(publicKey);
    Log.d(TAG, "Witness " + publicKey + " successfully scanned");
    Toast.makeText(getApplication(), R.string.witness_scan_success, Toast.LENGTH_SHORT).show();
    disposables.add(
        updateLaoWitnesses()
            .subscribe(
                () -> {
                  String networkSuccess = "Witness " + publicKey + " successfully added to LAO";
                  Log.d(TAG, networkSuccess);
                  Toast.makeText(getApplication(), networkSuccess, Toast.LENGTH_SHORT).show();
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_update_lao)));
  }

  public Completable updateLaoWitnesses() {
    Log.d(TAG, "Updating lao witnesses ");

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Completable.error(new UnknownLaoException());
    }

    Channel channel = laoView.getChannel();
    KeyPair mainKey = keyManager.getMainKeyPair();
    long now = Instant.now().getEpochSecond();
    UpdateLao updateLao =
        new UpdateLao(
            mainKey.getPublicKey(), laoView.getCreation(), laoView.getName(), now, witnesses);
    MessageGeneral msg = new MessageGeneral(mainKey, updateLao, gson);

    return networkManager
        .getMessageSender()
        .publish(channel, msg)
        .doOnComplete(() -> Log.d(TAG, "updated lao witnesses"))
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
        .doOnComplete(() -> Log.d(TAG, "updated lao with " + stateLao));
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.clear();
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    return null;
  }
}
