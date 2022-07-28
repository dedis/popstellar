package com.github.dedis.popstellar.model.objects.digitalcash;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.*;
import java.util.stream.Collectors;

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
    this.inputs = inputs;
    this.outputs = outputs;
    this.lockTime = lockTime;
    this.transactionId = transactionId;
  }

  public TransactionObject(TransactionObject transactionObject) {
    this.channel = new Channel(transactionObject.channel);
    this.version = transactionObject.version;
    this.inputs =
        transactionObject.inputs.stream().map(InputObject::new).collect(Collectors.toList());
    this.outputs =
        transactionObject.outputs.stream().map(OutputObject::new).collect(Collectors.toList());
    this.lockTime = transactionObject.lockTime;
    this.transactionId = transactionObject.transactionId;
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
    List<PublicKey> senders = new ArrayList<>();

    // Through the inputs look at the sender
    for (InputObject inpObj : getInputs()) {
      senders.add(inpObj.getScript().getPubKey());
    }

    return senders;
  }

  /**
   * Function that gives the Public Key Hashes of the Outputs
   *
   * @return List<String> outputs Public Key Hashes
   */
  public List<String> getReceiversHashTransaction() {
    List<String> receiverHash = new ArrayList<>();

    for (OutputObject outObj : getOutputs()) {
      receiverHash.add(outObj.getPubKeyHash());
    }

    return receiverHash;
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
   * Function that given a Public Key gives the miniLaoCoin received
   *
   * @param receiver Public Key of a potential receiver
   * @return int amount of Lao Coin
   */
  public long getMiniLaoPerReceiver(PublicKey receiver) {
    // Check in the future if useful
    if (!isReceiver(receiver)) {
      throw new IllegalArgumentException(
          "The public Key is not contained in the receiver public key");
    }
    // Set the return value to nothing
    long miniLao = 0;
    // Compute the hash of the public key
    String hashKey = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    for (OutputObject outObj : getOutputs()) {
      if (outObj.getScript().getPubKeyHash().equals(hashKey)) {
        miniLao += outObj.getValue();
      }
    }

    return miniLao;
  }

  /**
   * Total MiniLao per public key of a List of Transaction
   *
   * @param transaction List<TransactionObject>
   * @param receiver Public Key
   * @return long amount per user
   */
  public static long getMiniLaoPerReceiverSetTransaction(
      List<TransactionObject> transaction, PublicKey receiver) {
    return transaction.stream().mapToLong(obj -> obj.getMiniLaoPerReceiver(receiver)).sum();
  }

  /**
   * Function which return the first amount which correspond to the Public Key (this function is
   * useful if someone send money to herself, in fact only the first amount in the transaction
   * correspond to the money he has send to him)
   *
   * @param receiver Public Key of a potential receiver
   * @return int amount of Lao Coin
   */
  public long getMiniLaoPerReceiverFirst(PublicKey receiver) {
    // Check in the future if useful
    if (!isReceiver(receiver)) {
      throw new IllegalArgumentException(
          "The public Key is not contained in the receiver public key");
    }
    // Compute the hash of the public key
    String computeHash = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    for (OutputObject outObj : getOutputs()) {
      if (outObj.getPubKeyHash().equals(computeHash)) {
        // return after first occurrence
        return outObj.getValue();
      }
    }
    return 0;
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
   * Class which return the last roll call open
   *
   * @return Rollcall the roll call with the last ending tim e
   */
  public static TransactionObject lastLockedTransactionObject(
      List<TransactionObject> listTransaction) {
    Optional<TransactionObject> transactionObject =
        listTransaction.stream().max(Comparator.comparing(TransactionObject::getLockTime));
    if (!transactionObject.isPresent()) {
      throw new IllegalStateException();
    }
    return transactionObject.get();
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
