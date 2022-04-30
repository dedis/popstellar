package com.github.dedis.popstellar.ui.digitalcash;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransaction;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptTxIn;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptTxOut;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.TxIn;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.TxOut;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class DigitalCashViewModel extends AndroidViewModel {
    public static final String TAG = DigitalCashViewModel.class.getSimpleName();
  private static final String LAO_FAILURE_MESSAGE = "failed to retrieve lao";
  private static final int VERSION = 1;
  private static final String TYPE = "2";
  private static final String PUBLISH_MESSAGE = "sending publish message";
  // TODO : Name of the channel
  private static final String DIGITAL_CASH = "digital_cash";

    /*
     * LiveData objects for capturing events
     */
    private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenHistoryEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenSendEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiveEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenIssueEvent = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Boolean>> mOpenReceiptEvent = new MutableLiveData<>();

  private final LiveData<List<Lao>> mLAOs;
  private final MutableLiveData<String> mLaoId = new MutableLiveData<>();
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();

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
    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            this.laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
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

  public void setLaoId(String laoId) {
    mLaoId.setValue(laoId);
  }

  public void setLaoName(String laoName) {
    mLaoName.setValue(laoName);
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

  private void postTransaction(
      Map<KeyPair, Integer> user_amount,
      PublicKey sender,
      @Nullable Transaction previous_transaction,
      long timestamp) {
    Log.d(TAG, "Post a transaction");
    Lao lao = getCurrentLao();
    if (lao == null) {
      Log.e(TAG, LAO_FAILURE_MESSAGE);
      return;
    }

    // Required for the signature of the all the txin in txout
    List<String> list_txOutHash = new ArrayList<String>();
    List<Integer> list_txOutIndex = new ArrayList<Integer>();
    List<Integer> list_txoutvalue = new ArrayList<Integer>();
    // String = Base64(ScriptTxOut)
    List<String> list_txScript = new ArrayList<String>();

    // The list of TxOut
    // The list of TxIn
    List<TxOut> list_txout = new ArrayList<TxOut>();
    List<TxIn> list_txin = new ArrayList<TxIn>();

    for (Map.Entry<KeyPair, Integer> entry : user_amount.entrySet()) {
      // Public Key Hash
      try {
        // find out the hash of the recipient
        String pub_key_hash = toHexString(getSHA(entry.getKey().getPublicKey().toString()));

        // if is not the first transaction
        if (previous_transaction != null) {
          Iterator<TxOut> txOutIterator = previous_transaction.getTxOuts().iterator();
          int index = 0;
          boolean find_value = false;
          while ((!find_value) && txOutIterator.hasNext()) {
            TxOut next_txout = txOutIterator.next();
            if (next_txout.getScript().getPub_key_hash().equals(pub_key_hash)) {
              find_value = true;
              list_txOutIndex.add(index);
              // TODO : Hash next_txout (sha ?)
              list_txOutHash.add(toHexString(getSHA(next_txout.toString())));
            }
          }

          if (!find_value) {
            // we have a problem the previous transaction does not ref to the previous user
            // through an error
          }
        }

        // TxOut
        ScriptTxOut scriptTxOut = new ScriptTxOut(TYPE, pub_key_hash);
        // add script base64
        list_txScript.add(Base64.getEncoder().encodeToString(scriptTxOut.toString().getBytes()));
        // add the value
        list_txoutvalue.add(entry.getValue());
        list_txout.add(new TxOut(entry.getValue(), scriptTxOut));

      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
    }

    String string_sig = "";
    for (int i = 0; i < list_txOutHash.size(); i++) {
      string_sig = string_sig + list_txOutHash.get(i) + list_txOutIndex.get(i).toString();
    }
    for (int i = 0; i < list_txoutvalue.size(); i++) {
      string_sig = string_sig + list_txoutvalue.get(i).toString() + list_txScript.get(i);
    }

    int i = 0;
    for (Map.Entry<KeyPair, Integer> entry : user_amount.entrySet()) {
      try {
        // find out the hash of the recipient
        String pub_key_hash = toHexString(getSHA(entry.getKey().getPublicKey().toString()));

        // TODO : Problem how to sign with the private key
        // How do we get Key Pair

        ScriptTxIn scriptTxIn =
            new ScriptTxIn(
                TYPE, pub_key_hash, Base64.getUrlEncoder().encodeToString(string_sig.getBytes()));

        list_txin.add(new TxIn(list_txOutHash.get(i), list_txOutIndex.get(i), scriptTxIn));

      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
      i++;
    }
    Transaction transaction = new Transaction(VERSION, list_txin, list_txout, timestamp);
    PostTransaction postTransaction = new PostTransaction(transaction);
    try {
      PoPToken token = keyManager.getValidPoPToken(lao);
      Channel channel =
          lao.getChannel().subChannel(DIGITAL_CASH).subChannel(token.getPublicKey().getEncoded());
      Log.d(TAG, PUBLISH_MESSAGE);
      MessageGeneral msg = new MessageGeneral(token, postTransaction, gson);

      Disposable disposable =
          networkManager
              .getMessageSender()
              .publish(token, channel, postTransaction)
              .subscribe(
                  () -> {
                    Log.d(TAG, "Post transaction with id : " + msg.getMessageId());
                    Toast.makeText(
                            getApplication().getApplicationContext(),
                            "Post Transaction",
                            Toast.LENGTH_LONG)
                        .show();
                  },
                  error ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_post_transaction));
      disposables.add(disposable);
    } catch (KeyException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token);
    }
  }

  public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
    // Static getInstance method is called with hashing SHA
    MessageDigest md = MessageDigest.getInstance("SHA-256");

    // digest() method called
    // to calculate message digest of an input
    // and return array of byte
    return md.digest(input.getBytes(StandardCharsets.UTF_8));
  }

  public static String toHexString(byte[] hash) {
    // Convert byte array into signum representation
    BigInteger number = new BigInteger(1, hash);

    // Convert message digest into hex value
    StringBuilder hexString = new StringBuilder(number.toString(16));

    // Pad with leading zeros
    while (hexString.length() < 64) {
      hexString.insert(0, '0');
    }

    return hexString.toString();
  }
}
