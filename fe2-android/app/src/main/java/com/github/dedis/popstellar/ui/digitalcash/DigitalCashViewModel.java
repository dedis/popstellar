package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {

  public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final String PUBLISH_MESSAGE = "sending publish message";
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
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private final MutableLiveData<String> mRollCallId = new MutableLiveData<>();
  private final MutableLiveData<Integer> currentSelectedItem =
      new MutableLiveData<>(R.id.digital_cash_home_menu);

  /* Is used to change the lao Coin amount on the home fragment*/
  private final MutableLiveData<SingleEvent<Boolean>> updateLaoCoinEvent = new MutableLiveData<>();

  /* Update the receipt after sending a transactionn*/
  private final MutableLiveData<SingleEvent<String>> updateReceiptAddressEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> updateReceiptAmountEvent =
      new MutableLiveData<>();

  private final MutableLiveData<Lao> mCurrentLao = new MutableLiveData<>();

  private final MutableLiveData<Set<PoPToken>> mTokens = new MutableLiveData<>(new HashSet<>());
  private final LiveData<List<TransactionObject>> mTransactionHistory;

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;
  private final Wallet wallet;
  private final CompositeDisposable disposables;

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

    disposables = new CompositeDisposable();

    mTransactionHistory =
        Transformations.map(
            mCurrentLao,
            lao -> {
              try {
                if (lao == null) return new ArrayList<>();
                List<TransactionObject> historyList =
                    lao.getTransactionHistoryByUser()
                        .get(keyManager.getValidPoPToken(lao).getPublicKey());
                if (historyList == null) {
                  return new ArrayList<>();
                }
                return new ArrayList<>(historyList);
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

  public MutableLiveData<Integer> getCurrentSelectedItem() {
    return currentSelectedItem;
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
  public void setCurrentSelectedItem(@IdRes int item) {
    currentSelectedItem.postValue(item);
  }

  public void openHome() {
    setCurrentSelectedItem(R.id.digital_cash_home_menu);
  }

  public PublicKey getPublicKeyOutString(String encodedPub) throws NoRollCallException {
    for (PublicKey current : getAttendeesFromTheRollCall()) {
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
   */
  public void postTransaction(
      Map<String, String> receiverandvalue, long locktime, boolean coinBase) {

    /* Check if a Lao exist */
    Lao lao = getCurrentLaoValue();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    try {
      PrivateKey privK;
      PublicKey pubK;
      PoPToken token = keyManager.getValidPoPToken(lao);

      if (coinBase) {
        KeyPair kp = keyManager.getMainKeyPair();
        privK = kp.getPrivateKey();
        pubK = kp.getPublicKey();
      } else {
        privK = token.getPrivateKey();
        pubK = token.getPublicKey();
      }

      // first make the output
      List<Output> outputs = new ArrayList<>();
      long amountFromReceiver = 0;
      for (Map.Entry<String, String> current : receiverandvalue.entrySet()) {
        amountFromReceiver += computeOutputs(current, outputs);
      }

      // Then make the inputs
      // First there would be only one Input

      // Case no transaction before
      String transactionHash = TransactionObject.TX_OUT_HASH_COINBASE;
      int index = 0;

      List<Input> inputs = new ArrayList<>();
      if (getCurrentLaoValue().getTransactionByUser().containsKey(pubK) && !coinBase) {
        processNotCoinbaseTransaction(privK, pubK, outputs, amountFromReceiver, inputs);
      } else {
        inputs.add(
            processSignInput(
                privK,
                pubK,
                outputs,
                Collections.singletonMap(transactionHash, index),
                transactionHash));
      }

      Transaction transaction = new Transaction(VERSION, inputs, outputs, locktime);

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
                            R.string.digital_cash_post_transaction,
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
    } catch (KeyException | GeneralSecurityException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }
  }

  private Input processSignInput(
      PrivateKey privK,
      PublicKey pubK,
      List<Output> outputs,
      Map<String, Integer> transactionInpMap,
      String currentHash)
      throws GeneralSecurityException {
    Signature sig =
        privK.sign(
            new Base64URLData(
                Transaction.computeSigOutputsPairTxOutHashAndIndex(outputs, transactionInpMap)
                    .getBytes(StandardCharsets.UTF_8)));
    return new Input(
        currentHash, transactionInpMap.get(currentHash), new ScriptInput(TYPE, pubK, sig));
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

  public void setRollCallId(String rollCallId) {
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
  public Set<PublicKey> getAttendeesFromTheRollCall() throws NoRollCallException {
    return getCurrentLaoValue().lastRollCallClosed().getAttendees();
  }

  @Nullable
  public PublicKey getOrganizer() {
    return getCurrentLaoValue().getOrganizer();
  }

  @Nullable
  public List<String> getAttendeesFromTheRollCallList() throws NoRollCallException {
    return getAttendeesFromTheRollCall().stream()
        .map(Base64URLData::getEncoded)
        .collect(Collectors.toList());
  }

  public MutableLiveData<Lao> getCurrentLao() {
    return mCurrentLao;
  }

  public Lao getCurrentLaoValue() {
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
                    mTokens.getValue().add(token);
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

  public void savePersistentData() {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  private void processNotCoinbaseTransaction(
      PrivateKey privK,
      PublicKey pubK,
      List<Output> outputs,
      long amountFromReceiver,
      List<Input> inputs)
      throws GeneralSecurityException {
    int index;
    String transactionHash;
    List<TransactionObject> transactions = getCurrentLaoValue().getTransactionByUser().get(pubK);

    long amountSender =
        TransactionObject.getMiniLaoPerReceiverSetTransaction(transactions, pubK)
            - amountFromReceiver;
    Output outputSender = new Output(amountSender, new ScriptOutput(TYPE, pubK.computeHash()));
    outputs.add(outputSender);
    Map<String, Integer> transactionInpMap = new HashMap<>();
    for (TransactionObject transactionPrevious : transactions) {
      transactionHash = transactionPrevious.getTransactionId();
      index = transactionPrevious.getIndexTransaction(pubK);
      transactionInpMap.put(transactionHash, index);
    }

    for (String currentHash : transactionInpMap.keySet()) {
      inputs.add(processSignInput(privK, pubK, outputs, transactionInpMap, currentHash));
    }
  }

  public LiveData<List<TransactionObject>> getTransactionHistory() {
    return mTransactionHistory;
  }
}
