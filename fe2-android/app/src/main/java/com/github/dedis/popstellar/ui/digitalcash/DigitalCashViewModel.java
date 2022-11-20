package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.navigation.NavigationViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class DigitalCashViewModel extends NavigationViewModel<DigitalCashTab> {

  public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String RECEIVER_KEY_ERROR = "Error on the receiver s public key";
  private static final String COIN = "coin";

  private static final String TYPE = "P2PKH";
  private static final int VERSION = 1;

  public static final int NOTHING_SELECTED = -1;
  public static final int MIN_LAO_COIN = 0;

  /*
   * LiveData objects for capturing events
   */
  private final MutableLiveData<SingleEvent<Boolean>> postTransactionEvent =
      new MutableLiveData<>();

  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mRollCallId = new MutableLiveData<>();

  /* Is used to change the lao Coin amount on the home fragment*/
  private final MutableLiveData<SingleEvent<Boolean>> updateLaoCoinEvent = new MutableLiveData<>();

  /* Update the receipt after sending a transaction */
  private final MutableLiveData<SingleEvent<String>> updateReceiptAddressEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> updateReceiptAmountEvent =
      new MutableLiveData<>();

  private final MutableLiveData<LaoView> mCurrentLao = new MutableLiveData<>();
  private final MutableLiveData<String> mPageTitle = new MutableLiveData<>();

  private final MutableLiveData<Set<PoPToken>> mTokens = new MutableLiveData<>(new HashSet<>());
  private final LiveData<Set<TransactionObject>> mTransactionHistory;

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final Wallet wallet;
  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public DigitalCashViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager,
      Wallet wallet) {
    super(application);
    this.laoRepository = laoRepository;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;
    this.wallet = wallet;

    mTransactionHistory =
        Transformations.map(
            mCurrentLao,
            laoView -> {
              try {
                if (laoView == null) return new HashSet<>();
                Set<TransactionObject> historySet =
                    laoView
                        .getTransactionHistoryByUser()
                        .get(keyManager.getValidPoPToken(laoView).getPublicKey());
                if (historySet == null) {
                  return new HashSet<>();
                }
                return new HashSet<>(historySet);
              } catch (KeyException e) {
                Log.d(TAG, "error retrieving token: " + e);
                return null;
              }
            });
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  public MutableLiveData<String> getPageTitle() {
    return mPageTitle;
  }

  public void setPageTitle(String title) {
    mPageTitle.postValue(title);
  }

  public LiveData<SingleEvent<Boolean>> getPostTransactionEvent() {
    return postTransactionEvent;
  }

  public void postTransactionEvent() {
    postTransactionEvent.postValue(new SingleEvent<>(true));
  }

  public void updateLaoCoinEvent() {
    updateLaoCoinEvent.postValue(new SingleEvent<>(true));
  }

  public LiveData<SingleEvent<String>> getUpdateReceiptAddressEvent() {
    return updateReceiptAddressEvent;
  }

  public void updateReceiptAddressEvent(String address) {
    updateReceiptAddressEvent.postValue(new SingleEvent<>(address));
  }

  public LiveData<SingleEvent<String>> getUpdateReceiptAmountEvent() {
    return updateReceiptAmountEvent;
  }

  public void updateReceiptAmountEvent(String amount) {
    updateReceiptAmountEvent.postValue(new SingleEvent<>(amount));
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

  public MutableLiveData<Set<PoPToken>> getTokens() {
    return mTokens;
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  public PublicKey getPublicKeyOutString(String encodedPub) throws NoRollCallException {
    for (PublicKey current : Objects.requireNonNull(getAttendeesFromLastRollCall())) {
      if (current.getEncoded().equals(encodedPub)) {
        return current;
      }
    }
    return null;
  }

  private long computeOutputs(Map.Entry<String, String> current, List<Output> outputs) {
    try {
      PublicKey pub = getPublicKeyOutString(current.getKey());
      long amount = Long.parseLong(current.getValue());
      Output addOutput = new Output(amount, new ScriptOutput(TYPE, pub.computeHash()));
      outputs.add(addOutput);
      return amount;
    } catch (Exception e) {
      Log.e(TAG, RECEIVER_KEY_ERROR, e);
      return 0;
    }
  }

  /**
   * Post a transaction to your channel
   *
   * <p>Publish a Message General containing a PostTransaction data
   *
   * @return a Single emitting the sent transaction object
   */
  public Completable postTransaction(
      Map<String, String> receiverValues, long lockTime, boolean coinBase) {

    /* Check if a Lao exist */
    LaoView laoView = getCurrentLaoValue();
    if (laoView == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    // Find correct keypair
    return Single.fromCallable(
            () -> coinBase ? keyManager.getMainKeyPair() : keyManager.getValidPoPToken(laoView))
        .flatMapCompletable(
            keyPair -> {
              PostTransactionCoin postTxn =
                  createPostTransaction(keyPair, receiverValues, lockTime, coinBase);
              MessageGeneral msg = new MessageGeneral(keyPair, postTxn, gson);
              Channel channel = laoView.getChannel().subChannel(COIN);
              return networkManager
                  .getMessageSender()
                  .publish(channel, msg)
                  .doOnComplete(
                      () -> Log.d(TAG, "Successfully sent post transaction message : " + postTxn));
            });
  }

  private PostTransactionCoin createPostTransaction(
      KeyPair keyPair, Map<String, String> receiverValues, long lockTime, boolean coinBase)
      throws GeneralSecurityException {
    // first make the output
    List<Output> outputs = new ArrayList<>();
    long amountFromReceiver = 0;
    for (Map.Entry<String, String> current : receiverValues.entrySet()) {
      amountFromReceiver += computeOutputs(current, outputs);
    }

    // Then make the inputs
    // First there would be only one Input

    // Case no transaction before
    String transactionHash = TransactionObject.TX_OUT_HASH_COINBASE;
    int index = 0;

    List<Input> inputs = new ArrayList<>();
    if (getCurrentLaoValue().getTransactionByUser().containsKey(keyPair.getPublicKey())
        && !coinBase) {
      processNotCoinbaseTransaction(keyPair, outputs, amountFromReceiver, inputs);
    } else {
      inputs.add(
          processSignInput(
              keyPair, outputs, Collections.singletonMap(transactionHash, index), transactionHash));
    }

    Transaction transaction = new Transaction(VERSION, inputs, outputs, lockTime);

    return new PostTransactionCoin(transaction);
  }

  private Input processSignInput(
      KeyPair keyPair,
      List<Output> outputs,
      Map<String, Integer> transactionInpMap,
      String currentHash)
      throws GeneralSecurityException {
    Signature sig =
        keyPair.sign(
            new Base64URLData(
                Transaction.computeSigOutputsPairTxOutHashAndIndex(outputs, transactionInpMap)
                    .getBytes(StandardCharsets.UTF_8)));
    return new Input(
        currentHash,
        transactionInpMap.get(currentHash),
        new ScriptInput(TYPE, keyPair.getPublicKey(), sig));
  }

  public LiveData<String> getLaoId() {
    return mLaoId;
  }

  public void setLaoId(String laoId) {
    this.mLaoId.setValue(laoId);
  }

  public void setRollCallId(String rollCallId) {
    this.mRollCallId.setValue(rollCallId);
  }

  public LAORepository getLaoRepository() {
    return laoRepository;
  }

  public KeyManager getKeyManager() {
    return keyManager;
  }

  @Nullable
  public Set<PublicKey> getAttendeesFromLastRollCall() throws NoRollCallException {
    return getCurrentLaoValue().getMostRecentRollCall().getAttendees();
  }

  @Nullable
  public PublicKey getOrganizer() {
    return getCurrentLaoValue().getOrganizer();
  }

  @Nullable
  public List<String> getAttendeesFromTheRollCallList() throws NoRollCallException {
    return Objects.requireNonNull(getAttendeesFromLastRollCall()).stream()
        .map(Base64URLData::getEncoded)
        .collect(Collectors.toList());
  }

  public MutableLiveData<LaoView> getCurrentLao() {
    return mCurrentLao;
  }

  public LaoView getCurrentLaoValue() {
    return mCurrentLao.getValue();
  }

  public void subscribeToLao(String laoId) {
    disposables.add(
        laoRepository
            .getLaoObservable(laoId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                lao -> {
                  Log.d(
                      TAG,
                      "got an update for lao: "
                          + lao.getName()
                          + " transaction "
                          + lao.getTransactionHistoryByUser().toString());
                  mCurrentLao.postValue(lao);
                  try {
                    PoPToken token = keyManager.getValidPoPToken(lao);
                    Objects.requireNonNull(mTokens.getValue()).add(token);
                  } catch (KeyException e) {
                    Log.d(TAG, "Could not retrieve token");
                  }
                }));
  }

  public boolean canPerformTransaction(
      String currentAmount, String currentPublicKeySelected, int radioGroup) {
    if ((currentAmount.isEmpty()) || (Integer.parseInt(currentAmount) < MIN_LAO_COIN)) {
      // create in View Model a function that toast : please enter amount
      requireToPutAnAmount();
      return false;
    } else if (currentPublicKeySelected.isEmpty() && (radioGroup == NOTHING_SELECTED)) {
      // create in View Model a function that toast : please enter key
      requireToPutLAOMember();
      return false;
    } else {
      return true;
    }
  }

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  private void processNotCoinbaseTransaction(
      KeyPair keyPair, List<Output> outputs, long amountFromReceiver, List<Input> inputs)
      throws GeneralSecurityException {
    int index;
    String transactionHash;
    Set<TransactionObject> transactions =
        getCurrentLaoValue().getTransactionByUser().get(keyPair.getPublicKey());

    long amountSender =
        TransactionObject.getMiniLaoPerReceiverSetTransaction(
                Objects.requireNonNull(transactions), keyPair.getPublicKey())
            - amountFromReceiver;
    Output outputSender =
        new Output(amountSender, new ScriptOutput(TYPE, keyPair.getPublicKey().computeHash()));
    outputs.add(outputSender);
    Map<String, Integer> transactionInpMap = new HashMap<>();
    for (TransactionObject transactionPrevious : transactions) {
      transactionHash = transactionPrevious.getTransactionId();
      index = transactionPrevious.getIndexTransaction(keyPair.getPublicKey());
      transactionInpMap.put(transactionHash, index);
    }

    for (String currentHash : transactionInpMap.keySet()) {
      inputs.add(processSignInput(keyPair, outputs, transactionInpMap, currentHash));
    }
  }

  public LiveData<Set<TransactionObject>> getTransactionHistory() {
    return mTransactionHistory;
  }

  /**
   * This function should be used to add disposable object generated from subscription to sent
   * messages flows
   *
   * <p>They will be disposed of when the view model is cleaned which ensures that the subscription
   * stays relevant throughout the whole lifecycle of the activity and it is not bound to a fragment
   *
   * @param disposable to add
   */
  public void addDisposable(Disposable disposable) {
    this.disposables.add(disposable);
  }
}
