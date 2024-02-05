package com.github.dedis.popstellar.model.objects.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.InputObject
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.util.Collections
import java.util.Objects
import java.util.stream.Collectors

@Immutable
class TransactionObject(
    val channel: Channel,
    val version: Int,
    inputs: List<InputObject>,
    outputs: List<OutputObject>,
    val lockTime: Long,
    val transactionId: String
) {

  val inputs: List<InputObject> = Collections.unmodifiableList(ArrayList(inputs))
  val outputs: List<OutputObject> = Collections.unmodifiableList(ArrayList(outputs))

  val sendersTransaction: List<PublicKey>
    /**
     * Function that give the Public Key of the Inputs
     *
     * @return List<PublicKey> senders public keys
     */
    get() =
        inputs
            .stream()
            .map { input: InputObject -> input.script.pubKey }
            .collect(Collectors.toList())

  val receiversHashTransaction: List<String>
    /**
     * Function that gives the Public Key Hashes of the Outputs
     *
     * @return List<String> outputs Public Key Hashes
     */
    get() =
        outputs.stream().map { obj: OutputObject -> obj.pubKeyHash }.collect(Collectors.toList())

  /**
   * Function that gives the Public Keys of the Outputs
   *
   * @param mapHashKey Map<String,PublicKey> dictionary public key by public key hash
   * @return List<PublicKey> outputs public keys
   */
  fun getReceiversTransaction(mapHashKey: Map<String, PublicKey?>): List<PublicKey> {
    val receivers: MutableList<PublicKey> = ArrayList()

    for (transactionHash in receiversHashTransaction) {
      val pub =
          mapHashKey[transactionHash]
              ?: throw IllegalArgumentException("The hash correspond to no key in the dictionary")
      receivers.add(pub)
    }

    return receivers
  }

  /**
   * Check if a public key is in the recipient
   *
   * @param publicKey PublicKey of someone
   * @return true if public key in receiver, false otherwise
   */
  fun isReceiver(publicKey: PublicKey): Boolean {
    return receiversHashTransaction.contains(publicKey.computeHash())
  }

  /**
   * Check if a public key is in the senders
   *
   * @param publicKey PublicKey of someone
   * @return true if public key in receiver, false otherwise
   */
  fun isSender(publicKey: PublicKey): Boolean {
    return sendersTransaction.contains(publicKey)
  }

  /**
   * Function that given a Public Key gives the miniLaoCoin received for this transaction object
   *
   * @param user Public Key of a potential receiver
   * @return int amount of Lao Coin
   */
  fun getSumForUser(user: PublicKey): Long {
    // We are well aware that the logic could be compressed in a single filtering of outputs, but we
    // rejected it in favour of (some) clarity
    if (isCoinBaseTransaction) {
      // If it is an issuance, we return the sum of all output where the user is the recipient
      return outputs
          .stream()
          .filter { output: OutputObject -> output.isUserOutputRecipient(user) }
          .mapToLong { obj: OutputObject -> obj.value }
          .sum()
    }

    var sum = 0L
    if (isSender(user)) {
      // if the user is sender, we subtract the value of all output
      sum -= outputs.stream().mapToLong { obj: OutputObject -> obj.value }.sum()
    }

    // Regardless of if the user is the sender, we sum all output where the user is the recipient.
    // This is because of how the protocol is designed i.e. the sender will be in receivers as well
    sum +=
        outputs
            .stream()
            .filter { output: OutputObject -> output.isUserOutputRecipient(user) }
            .mapToLong { obj: OutputObject -> obj.value }
            .sum()

    return sum
  }

  /**
   * Function that return the index of the output for a given key in this Transaction
   *
   * @param publicKey PublicKey of an individual in Transaction output
   * @return int index in the transaction outputs
   */
  fun getIndexTransaction(publicKey: PublicKey): Int {
    val hashPubKey = publicKey.computeHash()

    for ((index, outObj) in outputs.withIndex()) {
      if (outObj.pubKeyHash == hashPubKey) {
        return index
      }
    }

    throw IllegalArgumentException(
        "this public key is not contained in the output of this transaction")
  }

  val isCoinBaseTransaction: Boolean
    /**
     * Function that return if a Transaction is a coin base transaction or not
     *
     * @return boolean true if coin base transaction
     */
    get() =
        sendersTransaction.size == 1 &&
            inputs[0].txOutHash == TX_OUT_HASH_COINBASE &&
            inputs[0].txOutIndex == 0

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as TransactionObject
    return transactionId == that.transactionId
  }

  override fun hashCode(): Int {
    return Objects.hash(transactionId)
  }

  override fun toString(): String {
    return "TransactionObject{channel=$channel, version=$version, inputs=$inputs, outputs=$outputs, " +
        "lockTime=$lockTime, transactionId='$transactionId'}"
  }

  companion object {
    const val TX_OUT_HASH_COINBASE = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
  }
}
