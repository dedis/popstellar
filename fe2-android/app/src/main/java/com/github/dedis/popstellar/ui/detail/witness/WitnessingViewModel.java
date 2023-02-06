package com.github.dedis.popstellar.ui.detail.witness;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;

@HiltViewModel
public class WitnessingViewModel extends AndroidViewModel {
  public static final String TAG = WitnessingViewModel.class.getSimpleName();

  private String laoId;

  private final LAORepository laoRepo;
  private final KeyManager keyManager;
  private final GlobalNetworkManager networkManager;

  @Inject
  public WitnessingViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      KeyManager keyManager,
      GlobalNetworkManager networkManager) {
    super(application);
    this.laoRepo = laoRepo;
    this.keyManager = keyManager;
    this.networkManager = networkManager;
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
}
