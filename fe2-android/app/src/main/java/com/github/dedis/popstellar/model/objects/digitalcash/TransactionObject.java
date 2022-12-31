package com.github.dedis.popstellar.model.objects.digitalcash;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.*;
import java.util.stream.Collectors;

@Immutable
public class TransactionObject {

  public static final String TX_OUT_HASH_COINBASE = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

  private final Channel channel;

  // version
  private final int version;

  // inputs
  private final List<InputObject> inputs;

  // outputs
  private final List<OutputObject> outputs;

  // lock_time
  private final long lockTime;

  private final String transactionId;

  public TransactionObject(
      Channel channel,
      int version,
      List<InputObject> inputs,
      List<OutputObject> outputs,
      long lockTime,
      String transactionId) {
    this.channel = channel;
    this.version = version;
    this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
    this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    this.lockTime = lockTime;
    this.transactionId = transactionId;
  }

  public Channel getChannel() {
    return channel;
  }

  public List<InputObject> getInputs() {
    return inputs;
  }

  public List<OutputObject> getOutputs() {
    return outputs;
  }

  public long getLockTime() {
    return lockTime;
  }

  public int getVersion() {
    return version;
  }

  /**
   * Function that give the Public Key of the Inputs
   *
   * @return List<PublicKey> senders public keys
   */
  public List<PublicKey> getSendersTransaction() {
    return inputs.stream().map(input -> input.getScript().getPubKey()).collect(Collectors.toList());
  }

  /**
   * Function that gives the Public Key Hashes of the Outputs
   *
   * @return List<String> outputs Public Key Hashes
   */
  public List<String> getReceiversHashTransaction() {
    return outputs.stream().map(OutputObject::getPubKeyHash).collect(Collectors.toList());
  }

  /**
   * Function that gives the Public Keys of the Outputs
   *
   * @param mapHashKey Map<String,PublicKey> dictionary public key by public key hash
   * @return List<PublicKey> outputs public keys
   */
  public List<PublicKey> getReceiversTransaction(Map<String, PublicKey> mapHashKey) {
    List<PublicKey> receivers = new ArrayList<>();
    for (String transactionHash : getReceiversHashTransaction()) {
      PublicKey pub = mapHashKey.getOrDefault(transactionHash, null);
      if (pub == null) {
        throw new IllegalArgumentException("The hash correspond to no key in the dictionary");
      }
      receivers.add(pub);
    }

    return receivers;
  }

  /**
   * Check if a public key is in the recipient
   *
   * @param publicKey PublicKey of someone
   * @return true if public key in receiver, false otherwise
   */
  public boolean isReceiver(PublicKey publicKey) {
    return getReceiversHashTransaction().contains(publicKey.computeHash());
  }

  /**
   * Check if a public key is in the senders
   *
   * @param publicKey PublicKey of someone
   * @return true if public key in receiver, false otherwise
   */
  public boolean isSender(PublicKey publicKey) {
    return getSendersTransaction().contains(publicKey);
  }

  /**
   * Function that given a Public Key gives the miniLaoCoin received for this transaction object
   *
   * @param user Public Key of a potential receiver
   * @return int amount of Lao Coin
   */
  public long getSumForUser(PublicKey user) {
    // We are well aware that the logic could be compressed in a single filtering of outputs, but we
    // rejected it in favour of (some) clarity

    if (isCoinBaseTransaction()) {
      // If it is an issuance, we return the sum of all output where the user is the recipient
      return getOutputs().stream()
          .filter(output -> output.isUserOutputRecipient(user))
          .mapToLong(OutputObject::getValue)
          .sum();
    }

    int sum = 0;
    if (isSender(user)) {
      // if the user is sender, we subtract the value of all output
      sum -= getOutputs().stream().mapToLong(OutputObject::getValue).sum();
    }

    // Regardless of if the user is the sender, we sum all output where the user is the recipient.
    // This is because of how the protocol is designed i.e. the sender will be in receivers as well
    sum +=
        getOutputs().stream()
            .filter(output -> output.isUserOutputRecipient(user))
            .mapToLong(OutputObject::getValue)
            .sum();
    return sum;
  }

  /**
   * Function that return the index of the output for a given key in this Transaction
   *
   * @param publicKey PublicKey of an individual in Transaction output
   * @return int index in the transaction outputs
   */
  public int getIndexTransaction(PublicKey publicKey) {
    String hashPubKey = publicKey.computeHash();
    int index = 0;
    for (OutputObject outObj : outputs) {
      if (outObj.getPubKeyHash().equals(hashPubKey)) {
        return index;
      }
      ++index;
    }
    throw new IllegalArgumentException(
        "this public key is not contained in the output of this transaction");
  }

  /**
   * Function that return if a Transaction is a coin base transaction or not
   *
   * @return boolean true if coin base transaction
   */
  public boolean isCoinBaseTransaction() {
    return (getSendersTransaction().size() == 1)
        && getInputs().get(0).getTxOutHash().equals(TX_OUT_HASH_COINBASE)
        && (getInputs().get(0).getTxOutIndex() == 0);
  }

  public String getTransactionId() {
    return transactionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionObject that = (TransactionObject) o;
    return transactionId.equals(that.transactionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionId);
  }

  @NonNull
  @Override
  public String toString() {
    return "TransactionObject{"
        + "channel="
        + channel
        + ", version="
        + version
        + ", inputs="
        + inputs.toString()
        + ", outputs="
        + outputs.toString()
        + ", lockTime="
        + lockTime
        + ", transactionId='"
        + transactionId
        + '\''
        + '}';
  }
}
