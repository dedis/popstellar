package com.github.dedis.popstellar.ui.lao.digitalcash

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.SingleEvent
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Input
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Output
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptInput
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptOutput
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction.Companion.computeSigOutputsPairTxOutHashAndIndex
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.Collections
import java.util.stream.Collectors
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class DigitalCashViewModel
@Inject
constructor(
    application: Application,
    /*
     * Dependencies for this class
     */
    val laoRepository: LAORepository,
    private val rollCallRepo: RollCallRepository,
    private val digitalCashRepo: DigitalCashRepository,
    private val witnessingRepo: WitnessingRepository,
    private val networkManager: GlobalNetworkManager,
    private val gson: Gson,
    private val keyManager: KeyManager
) : AndroidViewModel(application) {
  var laoId: String? = null

  /*
   * LiveData objects for capturing events
   */
  private val postTransactionEvent = MutableLiveData<SingleEvent<Boolean>>()

  /* Update the receipt after sending a transaction */
  private val updateReceiptAddressEvent = MutableLiveData<SingleEvent<String>>()
  private val updateReceiptAmountEvent = MutableLiveData<SingleEvent<String>>()

  fun getPostTransactionEvent(): LiveData<SingleEvent<Boolean>> {
    return postTransactionEvent
  }

  fun postTransactionEvent() {
    postTransactionEvent.postValue(SingleEvent(true))
  }

  fun getUpdateReceiptAddressEvent(): LiveData<SingleEvent<String>> {
    return updateReceiptAddressEvent
  }

  fun updateReceiptAddressEvent(address: String?) {
    updateReceiptAddressEvent.postValue(SingleEvent(address))
  }

  fun getUpdateReceiptAmountEvent(): LiveData<SingleEvent<String>> {
    return updateReceiptAmountEvent
  }

  fun updateReceiptAmountEvent(amount: String?) {
    updateReceiptAmountEvent.postValue(SingleEvent(amount))
  }

  private fun requireToPutAnAmount() {
    Toast.makeText(
            getApplication<Application>().applicationContext,
            R.string.digital_cash_amount_min_indication,
            Toast.LENGTH_LONG)
        .show()
  }

  private fun requireToPutLAOMember() {
    Toast.makeText(
            getApplication<Application>().applicationContext,
            R.string.digital_cash_select_lao_member_indication,
            Toast.LENGTH_LONG)
        .show()
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */
  @Throws(NoRollCallException::class)
  fun getPublicKeyOutString(encodedPub: String): PublicKey? {
    for (current in attendeesFromLastRollCall) {
      if (current.encoded == encodedPub) {
        return current
      }
    }
    return null
  }

  private fun computeOutputs(
      current: Map.Entry<String, String>,
      outputs: MutableList<Output>
  ): Long {
    return try {
      val pub = getPublicKeyOutString(current.key)
      val amount = current.value.toLong()
      val addOutput = Output(amount, ScriptOutput(TYPE, pub!!.computeHash()))
      outputs.add(addOutput)

      amount
    } catch (e: Exception) {
      Timber.tag(TAG).e(e, RECEIVER_KEY_ERROR)
      0
    }
  }

  /**
   * Post a transaction to your channel
   *
   * Publish a Message General containing a PostTransaction data
   *
   * @return a Single emitting the sent transaction object
   */
  fun postTransaction(
      receiverValues: Map<String, String>,
      lockTime: Long,
      coinBase: Boolean
  ): Completable {

    /* Check if a Lao exist */
    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          Timber.tag(TAG).e(e, LAO_FAILURE_MESSAGE)
          return Completable.error(UnknownLaoException())
        }

    // Find correct keypair
    return Single.fromCallable { if (coinBase) keyManager.mainKeyPair else validToken }
        .flatMapCompletable { keyPair: KeyPair ->
          val postTxn = createPostTransaction(keyPair, receiverValues, lockTime, coinBase)
          val msg = MessageGeneral(keyPair, postTxn, gson)
          val channel = laoView.channel.subChannel(COIN)

          networkManager.messageSender.publish(channel, msg).doOnComplete {
            Timber.tag(TAG).d("Successfully sent post transaction message : %s", postTxn)
          }
        }
  }

  @Throws(GeneralSecurityException::class)
  private fun createPostTransaction(
      keyPair: KeyPair,
      receiverValues: Map<String, String>,
      lockTime: Long,
      coinBase: Boolean
  ): PostTransactionCoin {
    // first make the output
    val outputs: MutableList<Output> = ArrayList()
    var amountFromReceiver: Long = 0
    for (current in receiverValues.entries) {
      amountFromReceiver += computeOutputs(current, outputs)
    }

    // Then make the inputs
    // First there would be only one Input

    // Case no transaction before
    val transactionHash = TransactionObject.TX_OUT_HASH_COINBASE
    val index = 0
    val inputs: MutableList<Input> = ArrayList()
    val transactions = getTransactionsForUser(keyPair.publicKey)
    if (transactions != null && !coinBase) {
      processNotCoinbaseTransaction(keyPair, outputs, amountFromReceiver, inputs)
    } else {
      inputs.add(
          processSignInput(
              keyPair, outputs, Collections.singletonMap(transactionHash, index), transactionHash))
    }
    val transaction = Transaction(VERSION, inputs, outputs, lockTime)

    return PostTransactionCoin(transaction)
  }

  @Throws(GeneralSecurityException::class)
  private fun processSignInput(
      keyPair: KeyPair,
      outputs: List<Output>,
      transactionInpMap: Map<String, Int>,
      currentHash: String
  ): Input {
    val sig =
        keyPair.sign(
            Base64URLData(
                computeSigOutputsPairTxOutHashAndIndex(outputs, transactionInpMap)
                    .toByteArray(StandardCharsets.UTF_8)))

    return Input(
        currentHash, transactionInpMap[currentHash]!!, ScriptInput(TYPE, keyPair.publicKey, sig))
  }

  @get:Throws(NoRollCallException::class)
  val attendeesFromLastRollCall: Set<PublicKey>
    get() = rollCallRepo.getLastClosedRollCall(laoId!!).attendees

  @get:Throws(UnknownLaoException::class)
  val organizer: PublicKey
    get() = lao.organizer

  @get:Throws(NoRollCallException::class)
  val attendeesFromTheRollCallList: List<String>
    get() =
        attendeesFromLastRollCall.stream().map(Base64URLData::encoded).collect(Collectors.toList())

  @get:Throws(UnknownLaoException::class)
  val lao: LaoView
    get() = laoRepository.getLaoView(laoId!!)

  val witnesses: Set<PublicKey>
    get() = witnessingRepo.getWitnesses(laoId!!)

  @get:Throws(KeyException::class)
  val validToken: PoPToken
    get() = keyManager.getValidPoPToken(laoId!!, rollCallRepo.getLastClosedRollCall(laoId!!))

  val ownKey: PublicKey
    get() = keyManager.mainPublicKey

  fun canPerformTransaction(
      currentAmount: String,
      currentPublicKeySelected: String,
      radioGroup: Int
  ): Boolean {
    if (currentAmount.isEmpty()) {
      // ask the user to fill the amount box
      requireToPutAnAmount()
    }

    val parsedAmount =
        try {
          currentAmount.toInt()
        } catch (e: NumberFormatException) {
          // Overflow in the amount (no characters or negative numbers can be inserted)
          logAndShow(
              getApplication<Application>().applicationContext,
              TAG,
              R.string.digital_cash_amount_inserted_error)
          return false
        }

    if (parsedAmount <= MIN_LAO_COIN) {
      logAndShow(
          getApplication<Application>().applicationContext,
          TAG,
          R.string.digital_cash_amount_min_indication)
      return false
    } else if (currentPublicKeySelected.isEmpty() && radioGroup == NOTHING_SELECTED) {
      // create in View Model a function that toast : please enter key
      requireToPutLAOMember()
      return false
    }

    return true
  }

  @Throws(GeneralSecurityException::class)
  private fun processNotCoinbaseTransaction(
      keyPair: KeyPair,
      outputs: MutableList<Output>,
      amountFromReceiver: Long,
      inputs: MutableList<Input>
  ) {
    var index: Int
    var transactionHash: String
    val transactions = getTransactionsForUser(keyPair.publicKey)!!

    val amountSender = getUserBalance(keyPair.publicKey) - amountFromReceiver
    val outputSender = Output(amountSender, ScriptOutput(TYPE, keyPair.publicKey.computeHash()))
    outputs.add(outputSender)

    val transactionInpMap: MutableMap<String, Int> = HashMap()
    for (transactionPrevious in transactions) {
      transactionHash = transactionPrevious.transactionId
      index = transactionPrevious.getIndexTransaction(keyPair.publicKey)
      transactionInpMap[transactionHash] = index
    }

    for (currentHash in transactionInpMap.keys) {
      inputs.add(processSignInput(keyPair, outputs, transactionInpMap, currentHash))
    }
  }

  private fun getTransactionsForUser(user: PublicKey): List<TransactionObject>? {
    return digitalCashRepo.getTransactions(laoId!!, user)
  }

  val transactionsObservable: Observable<List<TransactionObject>>
    get() =
        try {
          digitalCashRepo.getTransactionsObservable(laoId!!, validToken.publicKey)
        } catch (e: KeyException) {
          Observable.error(e)
        }

  val rollCallsObservable: Observable<Set<RollCall>>
    get() = rollCallRepo.getRollCallsObservableInLao(laoId!!)

  fun getUserBalance(user: PublicKey): Long {
    return digitalCashRepo.getUserBalance(laoId!!, user)
  }

  @get:Throws(KeyException::class)
  val ownBalance: Long
    get() = getUserBalance(validToken.publicKey)

  companion object {
    val TAG: String = DigitalCashViewModel::class.java.simpleName
    private const val LAO_FAILURE_MESSAGE = "failed to retrieve lao"
    private const val RECEIVER_KEY_ERROR = "Error on the receiver s public key"
    private const val COIN = "coin"
    private const val TYPE = "P2PKH"
    private const val VERSION = 1
    const val NOTHING_SELECTED = -1
    const val MIN_LAO_COIN = 0
  }
}
