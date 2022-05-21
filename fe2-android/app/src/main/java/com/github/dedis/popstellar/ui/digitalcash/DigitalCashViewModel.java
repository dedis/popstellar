package com.github.dedis.popstellar.ui.digitalcash;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptInput;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptOutput;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel
    implements CameraPermissionViewModel, QRCodeScanningViewModel {
  public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  private static final String COIN = "coin";
  private List<PublicKey> receivers = new ArrayList<>();

  private static final String TYPE = "Pay-to-Pubkey-Hash";
  private static final int VERSION = 1;
  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHistoryEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiveEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenIssueEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiptEvent = new MutableLiveData<>();

  private final LiveData<List<Lao>> mLAOs;
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();

  private final MutableLiveData<SingleEvent<HomeViewModel.HomeViewAction>> mSendTransactionEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mScanWarningEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mCoinSendingScanConfirmEvent =
      new MutableLiveData<>();
  private ScanningAction scanningAction;
  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;

  @Inject
  public DigitalCashViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager) {
    super(application);
    this.laoRepository = laoRepository;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;
    disposables = new CompositeDisposable();

    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            this.laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  /*
   * Getters for MutableLiveData instances declared above
   */
  public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenHistoryEvent() {
    return mOpenHistoryEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenSendEvent() {
    return mOpenSendEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenReceiveEvent() {
    return mOpenReceiveEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenIssueEvent() {
    return mOpenIssueEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenReceiptEvent() {
    return mOpenReceiptEvent;
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  public void openHome() {
    mOpenHomeEvent.postValue(new SingleEvent<>(true));
  }

  public void openHistory() {
    mOpenHistoryEvent.postValue(new SingleEvent<>(true));
  }

  public void openIssue() {
    mOpenIssueEvent.postValue(new SingleEvent<>(true));
  }

  public void openReceive() {
    mOpenReceiveEvent.postValue(new SingleEvent<>(true));
  }

  public void openSend() {
    mOpenSendEvent.postValue(new SingleEvent<>(true));
  }

  public void openReceipt() {
    mOpenReceiptEvent.postValue(new SingleEvent<>(true));
  }

  /**
   * Post a transaction to your channel
   *
   * <p>Publish a Message General containing a PostTransaction data
   */
  public void postTransaction(Map<PublicKey, Integer> receiverandvalue, long locktime) {
    Log.d(TAG, "Post a transaction");
    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    try {
      PoPToken token = keyManager.getValidPoPToken(lao);
      // first make the output
      List<Output> outputs = new ArrayList<>();
      for (Map.Entry<PublicKey, Integer> current : receiverandvalue.entrySet()) {
        Output add_output =
            new Output(current.getValue(), new ScriptOutput(TYPE, current.getKey().computeHash()));
        outputs.add(add_output);
      }

      // Then make the inputs
      // First there would be only one Input

      // Case no transaction before
      String transaction_hash = "";
      int index = 0;

      if (getCurrentLao().getTransactionByUser().containsKey(token.getPublicKey())) {
        transaction_hash =
            getCurrentLao().getTransactionByUser().get(token.getPublicKey()).computeId();
        index =
            getCurrentLao()
                .getTransactionByUser()
                .get(token.getPublicKey())
                .getIndexTransaction(token.getPublicKey());
      }
      String sig =
          Transaction.computeSigOutputsPairTxOutHashAndIndex(
              token, outputs, Collections.singletonMap(transaction_hash, index));
      Transaction transaction =
          new Transaction(
              VERSION,
              Collections.singletonList(
                  new Input(
                      transaction_hash,
                      index,
                      new ScriptInput(TYPE, token.getPublicKey().getEncoded(), sig))),
              outputs,
              locktime);

      PostTransactionCoin postTransactionCoin = new PostTransactionCoin(transaction);

      Channel channel =
          lao.getChannel().subChannel(COIN).subChannel(token.getPublicKey().getEncoded());

      Log.d(TAG, PUBLISH_MESSAGE);

      MessageGeneral msg = new MessageGeneral(token, postTransactionCoin, gson);

      Disposable disposable =
          networkManager
              .getMessageSender()
              .publish(token, channel, postTransactionCoin)
              .subscribe(
                  () -> {
                    Log.d(TAG, "Post transaction with the message id: " + msg.getMessageId());
                    Toast.makeText(
                            getApplication().getApplicationContext(),
                            "Post Transaction!",
                            Toast.LENGTH_LONG)
                        .show();
                  },
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_post_transaction));

      disposables.add(disposable);
    } catch (KeyException | GeneralSecurityException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }
  }

  public LiveData<SingleEvent<Boolean>> getCoinSendingScanConfirmEvent() {
    return mCoinSendingScanConfirmEvent;
  }

  public LiveData<SingleEvent<String>> getScanWarningEvent() {
    return mScanWarningEvent;
  }

  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<String> getLaoId() {
    return mLaoId;
  }

  public LiveData<String> getLaoName() {
    return mLaoName;
  }

  public void openQrCodeScanningCoinTransaction() {
    mSendTransactionEvent.setValue(new SingleEvent<>(HomeViewModel.HomeViewAction.SCAN));
  }

  public void openCameraPermission() {
    if (scanningAction == ScanningAction.SEND_COIN_ATTENDEE) {
      mSendTransactionEvent.setValue(
          new SingleEvent<>(HomeViewModel.HomeViewAction.REQUEST_CAMERA_PERMISSION));
    }
  }

  public void openScanning() {
    if (ContextCompat.checkSelfPermission(
            getApplication().getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      if (scanningAction == ScanningAction.SEND_COIN_ATTENDEE) {
        openQrCodeScanningCoinTransaction();
      }
    } else {
      openCameraPermission();
    }
  }

  public void setLaoId(String laoId) {
    this.mLaoId.setValue(laoId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
  }

  @Nullable
  public Lao getCurrentLao() {
    return getLao(getLaoId().getValue());
  }

  @Nullable
  private Lao getLao(String laoId) {
    LAOState laoState = laoRepository.getLaoById().get(laoId);
    if (laoState == null) return null;

    return laoState.getLao();
  }

  @Override
  public void onPermissionGranted() {
    if (scanningAction == ScanningAction.SEND_COIN_ATTENDEE) {
      openQrCodeScanningCoinTransaction();
    } else {
      throw new IllegalStateException("The permission should not occur when you not sending money");
    }
  }

  @Override
  public void onQRCodeDetected(Barcode barcode) {
    Log.d(TAG, "");
    PublicKey receiver;
    try {
      receiver = new PublicKey(barcode.rawValue);
    } catch (IllegalArgumentException e) {
      mScanWarningEvent.postValue(new SingleEvent<>("Invalid QR code. Please try again."));
      return;
    }

    if (receivers.contains(receiver)) {
      mScanWarningEvent.postValue(
          new SingleEvent<>("This QR code has already been scanned. Please try again."));
      return;
    }

    if (scanningAction == (ScanningAction.SEND_COIN_ATTENDEE)) {
      receivers.add(receiver);
      mCoinSendingScanConfirmEvent.postValue(new SingleEvent<>(true));
    }
  }

  @Override
  public int getScanDescription() {
    if (scanningAction == ScanningAction.SEND_COIN_ATTENDEE) {
      return R.string.qrcode_scanning_send_coin;
    } else {
      throw new IllegalStateException("The scanning should not occur when you not sending money");
    }
  }

  public void setScanningAction(ScanningAction scanningAction) {
    this.scanningAction = scanningAction;
  }

  @Override
  public ScanningAction getScanningAction() {
    return scanningAction;
  }
}
