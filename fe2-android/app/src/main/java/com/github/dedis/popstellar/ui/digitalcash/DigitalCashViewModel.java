package com.github.dedis.popstellar.ui.digitalcash;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.JsonParseException;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel
    implements CameraPermissionViewModel, QRCodeScanningViewModel {
  public static final String TAG = DigitalCashViewModel.class.getSimpleName();

  public enum DigitalCashViewAction {
    SCAN,
    REQUEST_CAMERA_PERMISSION
  }

  private static final ScanningAction scanningAction = ScanningAction.POST_TRANSACTION_PARTICIPANT;
  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHistoryEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiveEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenIssueEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiptEvent = new MutableLiveData<>();

  // private final MutableLiveData<SingleEvent<PublicKey>> mPkRollCall;

  // public LiveData<SingleEvent<PublicKey>> getPkRollCall() {
  // return mPkRollCall;
  // }

  @Inject
  public DigitalCashViewModel(@NonNull Application application) {
    super(application);
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

  public void openConnect() {
    if (checkSelfPermission(getApplication().getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      openQrCodeScanning();
    } else {
      openCameraPermission();
    }
  }

  @Override
  public void onPermissionGranted() {
    openQrCodeScanning();
  }

  @Override
  public void onQRCodeDetected(Barcode barcode) {
    Log.d(TAG, "Detected barcode with value: " + barcode.rawValue);
    ConnectToLao data;
    try {
      data = ConnectToLao.extractFrom(gson, barcode.rawValue);
    } catch (JsonParseException e) {
      Log.e(TAG, "Invalid QRCode data", e);
      Toast.makeText(
              getApplication().getApplicationContext(), "Invalid QRCode data", Toast.LENGTH_LONG)
          .show();
      return;
    }

  }

  @Override
  public int getScanDescription() {
    return R.string.qrcode_scanning_to_send_transaction;
  }

  @Override
  public ScanningAction getScanningAction() {
    return null;
  }
}
