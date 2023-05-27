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
import com.github.dedis.popstellar.repository.WitnessingRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

@HiltViewModel
public class WitnessingViewModel extends AndroidViewModel implements QRCodeScanningViewModel {
  private static final String TAG = WitnessingViewModel.class.getSimpleName();

  private String laoId;

  // Scanned witnesses, not necessarily accepted by LAO
  private final Set<PublicKey> scannedWitnesses = new HashSet<>();

  // Accepted witnesses
  private final MutableLiveData<List<PublicKey>> witnesses = new MutableLiveData<>();
  private final MutableLiveData<List<WitnessMessage>> witnessMessages = new MutableLiveData<>();
  private final MutableLiveData<Integer> nbScanned = new MutableLiveData<>(0);
  private MutableLiveData<Boolean> showPopup = new MutableLiveData<>(false);

  private final LAORepository laoRepo;
  private final WitnessingRepository witnessingRepo;
  private final KeyManager keyManager;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public WitnessingViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      WitnessingRepository witnessingRepo,
      KeyManager keyManager,
      GlobalNetworkManager networkManager,
      Gson gson) {
    super(application);
    this.laoRepo = laoRepo;
    this.witnessingRepo = witnessingRepo;
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

  public List<PublicKey> getScannedWitnesses() {
    return new ArrayList<>(scannedWitnesses);
  }

  /**
   * Function that initializes the view model. It sets the lao identifier and observe the witnesses
   * and witness messages.
   *
   * @param laoId identifier of the lao whose view model belongs
   */
  public void initialize(String laoId) {
    this.laoId = laoId;

    disposables.addAll(
        // Observe the witnesses
        witnessingRepo
            .getWitnessesObservableInLao(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                witnessesSet -> setWitnesses(new ArrayList<>(witnessesSet)),
                error ->
                    Timber.tag(TAG).d(error, "Error in updating the witnesses of lao %s", laoId)),
        // Observe the witness messages
        witnessingRepo
            .getWitnessMessagesObservableInLao(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                witnessMessage -> {
                  // Order by latest arrived
                  setWitnessMessages(
                      witnessMessage.stream()
                          .sorted(Comparator.comparing(WitnessMessage::getTimestamp).reversed())
                          .collect(Collectors.toList()));

                  // When a new witness message is received, if it needs to be signed by the user
                  // then we show a pop up that the user can click to open the witnessing fragment
                  PublicKey myPk = keyManager.getMainPublicKey();
                  if (!witnessMessage.isEmpty()
                      && witnessingRepo.isWitness(laoId, myPk)
                      && !Objects.requireNonNull(witnessMessages.getValue())
                          .get(0)
                          .getWitnesses()
                          .contains(myPk)) {
                    showPopup.setValue(true);
                  }
                },
                error ->
                    Timber.tag(TAG)
                        .d(error, "Error in updating the witness messages of lao %s", laoId)));
  }

  /**
   * This function deletes the messages that have already passed the witnessing policy to clear
   * useless space.
   */
  public void deleteSignedMessages() {
    Timber.tag(TAG).d("Deleting witnessing messages already signed by enough witnesses");
    witnessingRepo.deleteSignedMessages(laoId);
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

  public LiveData<Boolean> getShowPopup() {
    return showPopup;
  }

  public void disableShowPopup() {
    showPopup.setValue(false);
  }

  protected Completable signMessage(WitnessMessage witnessMessage) {
    Timber.tag(TAG).d("signing message with ID %s", witnessMessage.getMessageId());
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

              Timber.tag(TAG).d("Signed message id, resulting signature : %s", signature);
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
          getApplication().getApplicationContext(), TAG, e, R.string.qr_code_not_main_pk);
      return;
    }
    PublicKey publicKey = pkData.getPublicKey();
    if (scannedWitnesses.contains(publicKey)) {
      ErrorUtils.logAndShow(getApplication(), TAG, R.string.witness_already_scanned_warning);
      return;
    }

    scannedWitnesses.add(publicKey);
    nbScanned.setValue(scannedWitnesses.size());
    Timber.tag(TAG).d("Witness %s successfully scanned", publicKey);
    Toast.makeText(getApplication(), R.string.witness_scan_success, Toast.LENGTH_SHORT).show();
    // So far the update Lao it's not used, we simply add the witnesses at creation time
    /*
    disposables.add(
        updateLaoWitnesses()
            .subscribe(
                () -> {
                  String networkSuccess =
                      String.format(getApplication().getString(R.string.witness_added), publicKey);
                  Timber.tag(TAG).d(networkSuccess);
                  Toast.makeText(getApplication(), networkSuccess, Toast.LENGTH_SHORT).show();
                },
                error -> {
                  scannedWitnesses.remove(publicKey);
                  nbScanned.setValue(scannedWitnesses.size());
                  ErrorUtils.logAndShow(getApplication(), TAG, error, R.string.error_update_lao);
                }));
     */
  }

  private Completable updateLaoWitnesses() {
    Timber.tag(TAG).d("Updating lao witnesses ");

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
            mainKey.getPublicKey(),
            laoView.getCreation(),
            laoView.getName(),
            now,
            scannedWitnesses);
    MessageGeneral msg = new MessageGeneral(mainKey, updateLao, gson);

    return networkManager
        .getMessageSender()
        .publish(channel, msg)
        .doOnComplete(() -> Timber.tag(TAG).d("updated lao witnesses"))
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
        .doOnComplete(() -> Timber.tag(TAG).d("updated lao with %s", stateLao));
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }
}
