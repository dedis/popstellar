package com.github.dedis.student20_pop.home;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.data.LAORepository;
import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Broadcast;
import com.github.dedis.student20_pop.model.network.method.Publish;
import com.github.dedis.student20_pop.model.network.method.Subscribe;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.StateLao;
import com.github.dedis.student20_pop.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.student20_pop.utility.security.Keys;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeViewModel extends AndroidViewModel
    implements CameraPermissionViewModel, QRCodeScanningViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();
  public static final String SCAN = "SCAN";
  public static final String REQUEST_CAMERA_PERMISSION = "REQUEST_CAMERA_PERMISSION";

  private final MutableLiveData<Event<String>> mOpenLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mOpenConnectingEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<String>> mOpenConnectEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mOpenLaunchEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mLaunchNewLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mCancelNewLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<Event<Boolean>> mCancelConnectEvent = new MutableLiveData<>();
  private final MutableLiveData<String> mConnectingLao = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private final MutableLiveData<Map<String, Lao>> mLAOsById = new MutableLiveData<>();
  private final LiveData<List<Lao>> mLAOs =
      Transformations.map(mLAOsById, laosById -> new ArrayList<>(laosById.values()));
  private final Gson mGson;
  private final LAORepository mLAORepository;
  private Disposable disposable;
  private final AndroidKeysetManager mKeysetManager;

  public HomeViewModel(
      @NonNull Application application,
      Gson gson,
      LAORepository laoRepository,
      AndroidKeysetManager keysetManager) {
    super(application);

    mLAORepository = laoRepository;
    mGson = gson;
    mKeysetManager = keysetManager;

    subscribeToMessages();
  }

  @Override
  public void onPermissionGranted() {
    openQrCodeScanning();
  }

  @Override
  public int getScanDescription() {
    return R.string.qrcode_scanning_connect_lao;
  }

  @Override
  public void onQRCodeDetected(Barcode barcode) {
    Log.d(TAG, "Detected barcode with value: " + barcode.rawValue);

    // TODO: extract information
    setConnectingLao("lao id");
    openConnecting();

    // TODO: subscribe to the LAO

    // TODO: add LAO to the list of LAOs

    // TODO: initiate a catchup

    // openHome();
  }

  @Override
  protected void onCleared() {
    if (disposable != null) {
      disposable.dispose();
    }
  }

  public void setupDummyLAO() {
    Lao dummy =
            new Lao("dummy", "DEDIS");

    // TODO: add LAO to db
    // mLAORepository.addLAO(dummy.toLAO());

    Map<String, Lao> laosById = mLAOsById.getValue();
    if (laosById == null) {
      laosById = new HashMap<>();
    }
    laosById.put(dummy.getId(), dummy);

    mLAOsById.postValue(laosById);
  }

  public void subscribeToMessages() {
    disposable =
        Flowable.merge(mLAORepository.observeBroadcasts(), mLAORepository.observeResults())
            .subscribeOn(Schedulers.io())
            .subscribe(
                genericMessage -> {
                  if (genericMessage instanceof Result) {
                    handleResult((Result) genericMessage);
                  } else {
                    handleBroadcast((Broadcast) genericMessage);
                  }
                });
  }

  public void handleBroadcast(Broadcast broadcast) {
    // TODO: Verification
    Log.d(TAG, "Received a broadcast");

    MessageGeneral msg = broadcast.getMessage();
    String channel = broadcast.getChannel();

    if (!channel.startsWith("/root/")) {
      return;
    }

    String laoId = channel.substring(6);
    Data data = msg.getData();

    if (data instanceof StateLao) {
      StateLao stateLao = (StateLao) data;
      Map<String, Lao> laosById = mLAOsById.getValue();
      if (laosById == null) {
        laosById = new HashMap<>();
      }
      if (laosById.containsKey(laoId)) {
        Lao oldLao = laosById.get(laoId);

        // Long creation, Long lastModified, String organizer, List<String> witnesses
        Lao newLao =
            new Lao(stateLao);

        // We map things by the original LAO Id
        laosById.put(laoId, newLao);

        mLAOsById.postValue(laosById);
      }
    }
  }

  public void handleResult(Result result) {
    String serializedResult = mGson.toJson(result);
    Log.d(TAG, "Received a result " + serializedResult);
  }

  public void launchLao() {
    // Get organizer information and host
    // TODO: LaoRepository
    String laoName = mLaoName.getValue();
    try {
      KeysetHandle publicKeysetHandle = mKeysetManager.getKeysetHandle().getPublicKeysetHandle();

      String organizer = Keys.getEncodedKey(publicKeysetHandle);
      byte[] pkBuf = Base64.decode(organizer, Base64.NO_WRAP);

      PublicKeySign signer = mKeysetManager.getKeysetHandle().getPrimitive(PublicKeySign.class);

      CreateLao createLao = new CreateLao(laoName, organizer);
      MessageGeneral message = new MessageGeneral(pkBuf, createLao, signer, mGson);

      // TODO: get id from a global atomic counter
      Publish publish = new Publish("/root", 1, message);

      mLAORepository.sendMessage(publish);

      Subscribe subscribe = new Subscribe("/root/" + createLao.getId(), 2);
      mLAORepository.sendMessage(subscribe);

      Lao newLao = new Lao(laoName, organizer);
      Log.d(TAG, "Launch new LAO: " + laoName);

      // Save new lao
      Map<String, Lao> laosById = mLAOsById.getValue();
      if (laosById == null) {
        laosById = new HashMap<>();
      }
      laosById.put(newLao.getId(), newLao);
      mLAOsById.postValue(laosById);
    } catch (GeneralSecurityException e) {
      Log.e(TAG, "failed to get keyset handle", e);
    } catch (IOException e) {
      Log.e(TAG, "failed to parse public key", e);
    }
  }

  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<Event<String>> getOpenLaoEvent() {
    return mOpenLaoEvent;
  }

  public LiveData<Event<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<Event<Boolean>> getOpenConnectingEvent() {
    return mOpenConnectingEvent;
  }

  public LiveData<Event<String>> getOpenConnectEvent() {
    return mOpenConnectEvent;
  }

  public LiveData<Event<Boolean>> getOpenLaunchEvent() {
    return mOpenLaunchEvent;
  }

  public LiveData<Event<Boolean>> getLaunchNewLaoEvent() {
    return mLaunchNewLaoEvent;
  }

  public LiveData<Event<Boolean>> getCancelNewLaoEvent() {
    return mCancelNewLaoEvent;
  }

  public LiveData<Event<Boolean>> getCancelConnectEvent() {
    return mCancelConnectEvent;
  }

  public MutableLiveData<String> getConnectingLao() {
    return mConnectingLao;
  }

  public MutableLiveData<String> getLaoName() {
    return mLaoName;
  }

  public void openLAO(String laoId) {
    mOpenLaoEvent.setValue(new Event<>(laoId));
  }

  public void openHome() {
    mOpenHomeEvent.postValue(new Event<>(true));
  }

  public void openConnecting() {
    mOpenConnectingEvent.postValue(new Event<>(true));
  }

  public void openConnect() {
    if (ActivityCompat.checkSelfPermission(
            getApplication().getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      openQrCodeScanning();
    } else {
      openCameraPermission();
    }
  }

  public void openQrCodeScanning() {
    mOpenConnectEvent.setValue(new Event<>(SCAN));
  }

  public void openCameraPermission() {
    mOpenConnectEvent.setValue(new Event<>(REQUEST_CAMERA_PERMISSION));
  }

  public void openLaunch() {
    mOpenLaunchEvent.setValue(new Event<>(true));
  }

  public void launchNewLao() {
    mLaunchNewLaoEvent.setValue(new Event<>(true));
  }

  public void cancelNewLao() {
    mCancelNewLaoEvent.setValue(new Event<>(true));
  }

  public void cancelConnect() {
    mCancelConnectEvent.setValue(new Event<>(true));
  }

  public void setConnectingLao(String lao) {
    this.mConnectingLao.postValue(lao);
  }

  public void setLaoName(String name) {
    this.mLaoName.setValue(name);
  }
}
