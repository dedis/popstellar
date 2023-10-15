package com.github.dedis.popstellar.ui.lao.popcha;

import android.app.Application;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.data.popcha.PoPCHAAuthentication;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.PoPCHAQRCode;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.disposables.CompositeDisposable;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import timber.log.Timber;

@HiltViewModel
public class PoPCHAViewModel extends AndroidViewModel implements QRCodeScanningViewModel {

  private static final String TAG = PoPCHAViewModel.class.getSimpleName();

  private static final String AUTHENTICATION = "authentication";

  private String laoId;
  private final MutableLiveData<SingleEvent<String>> textDisplayed = new MutableLiveData<>();
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  private final CompositeDisposable disposables = new CompositeDisposable();

  /* Dependencies to inject */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;

  @Inject
  public PoPCHAViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      KeyManager keyManager) {
    super(application);
    this.laoRepository = laoRepository;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public String getLaoId() {
    return laoId;
  }

  public MutableLiveData<SingleEvent<String>> getTextDisplayed() {
    return textDisplayed;
  }

  private void postTextDisplayed(String text) {
    textDisplayed.postValue(new SingleEvent<>(text));
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  public void handleData(String data) {
    // Don't process another data from the scanner if I'm already trying to connect
    if (connecting.get()) {
      return;
    }
    connecting.set(true);
    PoPCHAQRCode popCHAQRCode;
    try {
      popCHAQRCode = new PoPCHAQRCode(data, laoId);
    } catch (IllegalArgumentException e) {
      Timber.tag(TAG).e(e, "Invalid QRCode PoPCHAData");
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.invalid_qrcode_popcha_data,
              Toast.LENGTH_LONG)
          .show();
      connecting.set(false);
      return;
    }
    PoPToken token;
    try {
      token = keyManager.getLongTermPoPToken(laoId, popCHAQRCode.getClientId());
    } catch (KeyException e) {
      Timber.tag(TAG).e(e, "Impossible to generate the token");
      connecting.set(false);
      return;
    }

    postTextDisplayed(popCHAQRCode.toString());
    try {
      sendAuthRequest(popCHAQRCode, token);
    } catch (GeneralSecurityException | UnknownLaoException e) {
      if (e instanceof GeneralSecurityException) {
        Timber.tag(TAG).e(e, "Impossible to generate sign the token");
      } else {
        Timber.tag(TAG).e(e, "Impossible to find lao");
      }
      connecting.set(false);
    }
  }

  private void sendAuthRequest(PoPCHAQRCode popCHAQRCode, PoPToken token)
      throws GeneralSecurityException, UnknownLaoException {
    Base64URLData nonce = new Base64URLData(popCHAQRCode.getNonce());
    Signature signedToken = token.sign(nonce);
    PoPCHAAuthentication authMessage =
        new PoPCHAAuthentication(
            popCHAQRCode.getClientId(),
            nonce.toString(),
            token.getPublicKey().getEncoded(),
            signedToken.getEncoded(),
            popCHAQRCode.getHost(),
            popCHAQRCode.getState(),
            popCHAQRCode.getResponseMode());
    Channel channel = getLao().getChannel().subChannel(AUTHENTICATION);
    disposables.add(
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), channel, authMessage)
            .subscribe(() -> Timber.tag(TAG).d("sent the auth message for popcha")));
  }

  public void disableConnectingFlag() {
    connecting.set(false);
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    // This is useless for the PoPCHA Scanner (we just scan once)
    return new MutableLiveData<>(0);
  }
}
