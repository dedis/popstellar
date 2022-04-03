package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.AddDummyTransaction;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.AddTransaction;
import com.github.dedis.popstellar.model.objects.Address;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {
  public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final CompositeDisposable disposables;

  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String PUBLISH_MESSAGE = "sending publish message";


  //TODO are we in a row call
  //@Inject RollCall rollCall;
  //@Inject Lao this_lao;

  private static final String DIGITAL_CASH = "DIGITAL_CASH";

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHistoryEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiveEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenIssueEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiptEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mSendNewDummyTransactionEvent = new MutableLiveData<>();

  private final LiveData<List<Lao>> mLAOs;
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();

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

    this.mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            this.laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
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

  public MutableLiveData<SingleEvent<Boolean>> getSendNewDummyTransactionEvent() {
    return mSendNewDummyTransactionEvent;
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

  public void setLaoId(String laoId) {
    mLaoId.setValue(laoId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
  }

  public void sendNewDummyTransactionEvent(){
    mSendNewDummyTransactionEvent.postValue(new SingleEvent<>(true));
  }

  /**
   * Send a coin to your own channel.
   *
   * <p>Publish a MessageGeneral containing AddChirp data.
   *
   * @param amount int
   * @param sender_address String
   * @param receiver_address String
   */

  public void sendNewDummyCoin(int amount , @Nullable Address sender_address, @Nullable Address receiver_address,Context context){
    Log.d(TAG, "Sending a dummy transaction");
    AddDummyTransaction addTransaction = new AddDummyTransaction(amount,sender_address,receiver_address);
    Toast.makeText(context, addTransaction.toString(), Toast.LENGTH_LONG).show();

    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      Toast.makeText(context, "FAIL DUMMY", Toast.LENGTH_LONG).show();
      return;
    }

    //AddDummyTransaction addTransaction = new AddDummyTransaction(amount,sender_address,receiver_address);
    //wallet
    try {
      KeyPair token = keyManager.getMainKeyPair();
      Toast.makeText(context,token.toString(),Toast.LENGTH_LONG).show();
              //.getValidPoPToken(lao);
              //,rollCall);
      Channel channel =
              lao.getChannel().subChannel(DIGITAL_CASH).subChannel(token.getPublicKey().getEncoded());
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(token, addTransaction, gson);

      Disposable disposable =
              networkManager
                      .getMessageSender()
                      .publish(token, channel, addTransaction)
                      .subscribe(
                              () -> Log.d(TAG, "sent some transaction" + msg.getMessageId()),
                              error ->
                                      ErrorUtils.logAndShow(
                                              getApplication(), TAG, error, R.string.error_sending_coin));
      disposables.add(disposable);
    } catch (Exception e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_for_keys);
    }
    Toast.makeText(context, "DONE DUMMY", Toast.LENGTH_LONG).show();
    //
    }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
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
}
