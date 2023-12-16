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
import com.github.dedis.popstellar.repository.RollCallRepository;
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

  public static final String AUTHENTICATION = "authentication";

  private String laoId;
  private final MutableLiveData<SingleEvent<String>> textDisplayed = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> isRequestCompleted = new MutableLiveData<>();
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  private final CompositeDisposable disposables = new CompositeDisposable();

  /* Dependencies to inject */
  private final LAORepository laoRepository;
  private final RollCallRepository rollCallRepo;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;

  @Inject
  public PoPCHAViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      RollCallRepository rollCallRepo,
      GlobalNetworkManager networkManager,
      KeyManager keyManager) {
    super(application);
    this.laoRepository = laoRepository;
    this.rollCallRepo = rollCallRepo;
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

  public MutableLiveData<SingleEvent<Boolean>> getIsRequestCompleted() {
    return isRequestCompleted;
  }

  public void deactivateRequestCompleted() {
    isRequestCompleted.postValue(new SingleEvent<>(false));
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
    AuthToken token;
    try {
      token = keyManager.getLongTermAuthToken(laoId, popCHAQRCode.clientId);
    } catch (KeyException e) {
      Timber.tag(TAG).e(e, "Impossible to generate the token");
      connecting.set(false);
      return;
    }

    try {
      sendAuthRequest(popCHAQRCode, token);
      postTextDisplayed(popCHAQRCode.toString());
      isRequestCompleted.postValue(new SingleEvent<>(true));
    } catch (GeneralSecurityException e) {
      Timber.tag(TAG).e(e, "Impossible to sign the token");
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.error_sign_message,
              Toast.LENGTH_LONG)
          .show();
    } catch (UnknownLaoException e) {
      Timber.tag(TAG).e(e, "Impossible to find the lao");
      Toast.makeText(
              getApplication().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } catch (KeyException e) {
      Timber.tag(TAG).e(e, "Impossible to get pop token: no roll call exists in the lao");
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.no_rollcall_exception,
              Toast.LENGTH_LONG)
          .show();
    } finally {
      connecting.set(false);
    }
  }

  private void sendAuthRequest(PoPCHAQRCode popCHAQRCode, AuthToken token)
      throws GeneralSecurityException, UnknownLaoException, KeyException {
    String nonce = Base64URLData.encode(popCHAQRCode.nonce);
    Signature signedToken = token.sign(new Base64URLData(nonce));
    PoPCHAAuthentication authMessage =
        new PoPCHAAuthentication(
            popCHAQRCode.clientId,
            nonce,
            token.publicKey,
            signedToken,
            popCHAQRCode.host,
            popCHAQRCode.state,
            popCHAQRCode.responseMode);
    Channel channel = getLao().getChannel().subChannel(AUTHENTICATION);
    disposables.add(
        networkManager
            .getMessageSender()
            .publish(getValidToken(), channel, authMessage)
            .subscribe(
                () -> Timber.tag(TAG).d("sent the auth message for popcha"),
                err -> Timber.tag(TAG).e(err, "error sending the auth message for popcha")));
  }

  private PoPToken getValidToken() throws KeyException {
    return keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId));
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    // This is useless for the PoPCHA Scanner (we just scan once)
    return new MutableLiveData<>(0);
  }
}
