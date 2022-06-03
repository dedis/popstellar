package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
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
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.Hash;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {
  public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  private static final String COIN = "coin";

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

  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private final MutableLiveData<String> mRollCallId = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> postTransactionEvent = new MutableLiveData<>();
  /* Is used to change the lao Coin amount on the home fragment*/
  private final MutableLiveData<SingleEvent<Boolean>> updateLaoCoinEvent = new MutableLiveData<>();
  /* Update the receipt after sending a transactionn*/
  private final MutableLiveData<SingleEvent<String>> updateReceiptAddressEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> updateReceiptAmountEvent =
      new MutableLiveData<>();

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
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  public LiveData<SingleEvent<Boolean>> getPostTransactionEvent() {
    return postTransactionEvent;
  }

  public void postTransactionEvent() {
    postTransactionEvent.postValue(new SingleEvent<>(true));
  }

  public LiveData<SingleEvent<Boolean>> getUpdateLaoCoinEvent() {
    return updateLaoCoinEvent;
  }

  public void updateLaoCoinEvent() {
    updateLaoCoinEvent.postValue(new SingleEvent<>(true));
  }

  public LiveData<SingleEvent<String>> getUpdateReceiptAddressEvent() {
    return updateReceiptAddressEvent;
  }

  public void updateReceiptAddressEvent(String Address) {
    updateReceiptAddressEvent.postValue(new SingleEvent<>(Address));
  }

  public LiveData<SingleEvent<String>> getUpdateReceiptAmountEvent() {
    return updateReceiptAmountEvent;
  }

  public void updateReceiptAmountEvent(String Amount) {
    updateReceiptAmountEvent.postValue(new SingleEvent<>(Amount));
  }

  public void requireToPutAnAmount() {
    Toast.makeText(
            getApplication().getApplicationContext(),
            "Please enter a positive amount of LAOcoin",
            Toast.LENGTH_LONG)
        .show();
  }

  public void requireToPutLAOMember() {
    Toast.makeText(
            getApplication().getApplicationContext(),
            "Please select a LAOMember",
            Toast.LENGTH_LONG)
        .show();
  }

  /*
   * Getters for MutableLiveData instances declared above
   *
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

  /** Post Transaction Test Try To follow the message */
  public void postTransactionTest(Map<String, String> PublicKeyAmount, Long time)
      throws KeyException {
    Log.d(TAG, "Post a transaction Test");
    KeyPair the_keys = keyManager.getValidPoPToken(getCurrentLao());

    List<Output> outputs =
        Collections.singletonList(
            new Output((long) 0, new ScriptOutput(TYPE, the_keys.getPublicKey().computeHash())));

    String transaction_hash = Hash.hash("string");
    int index = 0;

    String sig =
        Transaction.computeSigOutputsPairTxOutHashAndIndex(
            the_keys, outputs, Collections.singletonMap(transaction_hash, index));
    Transaction transaction =
        new Transaction(
            VERSION,
            Collections.singletonList(
                new Input(
                    transaction_hash,
                    index,
                    new ScriptInput(TYPE, the_keys.getPublicKey().getEncoded(), sig))),
            outputs,
            time);

    PostTransactionCoin postTransactionCoin = new PostTransactionCoin(transaction);
    Log.d(TAG, postTransactionCoin.toString());

    Channel channel =
        getCurrentLao()
            .getChannel()
            .subChannel(COIN);
    Log.d(TAG, channel.toString());

    // laoRepository.updateNodes(channel);

    Log.d(TAG, PUBLISH_MESSAGE);

    MessageGeneral msg = new MessageGeneral(the_keys, postTransactionCoin, gson);
    Log.d(TAG, msg.toString());

    Disposable disposable =
        networkManager
            .getMessageSender()
            .publish(the_keys, channel, postTransactionCoin)
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
  }

  public PublicKey getPublicKeyOutString(String encodedpub) throws Exception {
    Iterator<PublicKey> ite = getAttendeesFromTheRollCall().iterator();
    while (ite.hasNext()) {
      PublicKey current = ite.next();
      if (current.getEncoded().equals(encodedpub)) {
        return current;
      }
    }
    throw new Exception("Don't find the key");
  }

  /**
   * Post a transaction to your channel
   *
   * <p>Publish a Message General containing a PostTransaction data
   */
  public void postTransaction(Map<String, String> receiverandvalue, long locktime) {

    /* Check if a Lao exist */
    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    try {
      PoPToken token =
              keyManager.getValidPoPToken(lao);
      // first make the output
      List<Output> outputs = new ArrayList<>();
      long amountFromReceiver = 0;
      for (Map.Entry<String, String> current : receiverandvalue.entrySet()) {
        PublicKey pub = null;
        try {
          pub = getPublicKeyOutString(current.getKey());
        } catch (Exception e) {
          e.printStackTrace();
          Log.d(TAG, "Error on the key to whom we send !");
        }
        long amount = Long.valueOf(current.getValue());
        amountFromReceiver += amount;
        Output add_output = new Output(amount, new ScriptOutput(TYPE, pub.computeHash()));
        outputs.add(add_output);
      }

      // Then make the inputs
      // First there would be only one Input

      // Case no transaction before
      String transaction_hash = Hash.hash("none");
      int index = 0;

      if (getCurrentLao().getTransactionByUser().containsKey(token.getPublicKey())) {
        TransactionObject transactionPrevious =
            getCurrentLao().getTransactionByUser().get(token.getPublicKey());

        long amount_sender =
            transactionPrevious.getMiniLaoPerReceiver(token.getPublicKey()) - amountFromReceiver;
        Output output_sender =
            new Output(amount_sender, new ScriptOutput(TYPE, token.getPublicKey().computeHash()));
        outputs.add(output_sender);
        transaction_hash = transactionPrevious.computeId();
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

      Channel channel = lao.getChannel().subChannel(COIN);

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
                    Log.d(TAG, "The transaction send " + lao.getTransactionByUser().toString());
                    Log.d(
                        TAG,
                        "The transaction history " + lao.getTransactionHistoryByUser().toString());
                  },
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_post_transaction));

      disposables.add(disposable);
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }


  }


  public LiveData<String> getLaoId() {
    return mLaoId;
  }

  public LiveData<String> getLaoName() {
    return mLaoName;
  }

  public void setLaoId(String laoId) {
    this.mLaoId.setValue(laoId);
  }

  public void setRollCallId(String rollCallId){
    Log.d(TAG, "Set the rollcall Id : " + rollCallId);
    this.mRollCallId.setValue(rollCallId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
  }

  public LAORepository getLaoRepository() {
    return laoRepository;
  }

  public KeyManager getKeyManager() {
    return keyManager;
  }

  @Nullable
  public Lao getCurrentLao() {
    Log.d(TAG, "search the lao with id :" + getLaoId().getValue());
    return getLao(getLaoId().getValue());
  }

  @Nullable
  public Set<PublicKey> getAttendeesFromTheRollCall() throws NoRollCallException {
    return getCurrentLao().lastRollCallClosed().getAttendees();

  }

  @Nullable
  public PublicKey getOrganizer(){
    return getCurrentLao().getOrganizer();
  }

  @Nullable
  public List<String> getAttendeesFromTheRollCallList() throws NoRollCallException {
    List<String> list = new ArrayList<>();
    Iterator<PublicKey> pub = Objects.requireNonNull(getAttendeesFromTheRollCall()).iterator();
    while (pub.hasNext()){
      String current = pub.next().getEncoded();
      list.add(current);
    }
    return list;
  }

  @Nullable
  private Lao getLao(String laoId) {
    LAOState laoState = laoRepository.getLaoById().get(laoId);
    if (laoState == null) return null;
    return laoState.getLao();
  }
}
