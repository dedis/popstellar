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
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
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
import io.reactivex.Observable;
import io.reactivex.*;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {

  public static final String TAG = DigitalCashViewModel.class.getSimpleName();

  private String laoId;
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

  /* Update the receipt after sending a transaction */
  private final MutableLiveData<SingleEvent<String>> updateReceiptAddressEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> updateReceiptAmountEvent =
      new MutableLiveData<>();


  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepository;
  private final RollCallRepository rollCallRepo;
  private final DigitalCashRepository digitalCashRepo;
  private final GlobalNetworkManager networkManager;
  private final Gson gson;
  private final KeyManager keyManager;

  @Inject
  public DigitalCashViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      RollCallRepository rollCallRepo,
      DigitalCashRepository digitalCashRepo,
      GlobalNetworkManager networkManager,
      Gson gson,
      KeyManager keyManager) {
    super(application);
    this.laoRepository = laoRepository;
    this.rollCallRepo = rollCallRepo;
    this.digitalCashRepo = digitalCashRepo;
    this.networkManager = networkManager;
    this.gson = gson;
    this.keyManager = keyManager;
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public LiveData<SingleEvent<Boolean>> getPostTransactionEvent() {
    return postTransactionEvent;
  }

  public void postTransactionEvent() {
    postTransactionEvent.postValue(new SingleEvent<>(true));
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
    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return Completable.error(new UnknownLaoException());
    }

    // Find correct keypair
    return Single.fromCallable(() -> coinBase ? keyManager.getMainKeyPair() : this.getValidToken())
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
    List<TransactionObject> transactions = getTransactionsForUser(keyPair.getPublicKey());
    if (transactions != null && !coinBase) {
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

  public LAORepository getLaoRepository() {
    return laoRepository;
  }

  public PublicKey getOwnKey() {
    return keyManager.getMainPublicKey();
  }

  @Nullable
  public Set<PublicKey> getAttendeesFromLastRollCall() throws NoRollCallException {
    return rollCallRepo.getLastClosedRollCall(laoId).getAttendees();
  }

  @Nullable
  public PublicKey getOrganizer() throws UnknownLaoException {
    return getLao().getOrganizer();
  }

  @Nullable
  public List<String> getAttendeesFromTheRollCallList() throws NoRollCallException {
    return Objects.requireNonNull(getAttendeesFromLastRollCall()).stream()
        .map(Base64URLData::getEncoded)
        .collect(Collectors.toList());
  }

  public LaoView getLao() throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  public PoPToken getValidToken() throws KeyException {
    return keyManager.getValidPoPToken(laoId, rollCallRepo.getLastClosedRollCall(laoId));
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

  private void processNotCoinbaseTransaction(
      KeyPair keyPair, List<Output> outputs, long amountFromReceiver, List<Input> inputs)
      throws GeneralSecurityException {
    int index;
    String transactionHash;
    List<TransactionObject> transactions = getTransactionsForUser(keyPair.getPublicKey());

    long amountSender = getUserBalance(keyPair.getPublicKey()) - amountFromReceiver;
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

  public List<TransactionObject> getTransactionsForUser(PublicKey user) {
    return digitalCashRepo.getTransactions(laoId, user);
  }

  public Observable<List<TransactionObject>> getTransactionsObservable() {
    try {
      return digitalCashRepo.getTransactionsObservable(laoId, getValidToken().getPublicKey());
    } catch (KeyException e) {
      return Observable.error(e);
    }
  }

  public Observable<Set<RollCall>> getRollCallsObservable() {
    return rollCallRepo.getRollCallsObservableInLao(laoId);
  }

  public long getUserBalance(PublicKey user) {
    return digitalCashRepo.getUserBalance(laoId, user);
  }

  public long getOwnBalance() throws KeyException {
    return getUserBalance(getValidToken().getPublicKey());
  }
}
