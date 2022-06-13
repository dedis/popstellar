package com.github.dedis.popstellar.model.objects;

import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.security.Hash;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransactionObject {
  private Channel channel;

  // version
  private int version;

  // inputs
  private List<InputObject> inputs;

  // outputs
  private List<OutputObject> outputs;

  // lock_time
  private long lockTime;

  public TransactionObject() {
    /* Empty constructor == empty transaction */
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(@NonNull Channel channel) {
    this.channel = channel;
  }

  public List<InputObject> getInputs() {
    return inputs;
  }

  public void setInputs(List<InputObject> inputs) {
    this.inputs = inputs;
  }

  public List<OutputObject> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<OutputObject> outputs) {
    this.outputs = outputs;
  }

  public long getLockTime() {
    return lockTime;
  }

  public void setLockTime(long lockTime) {
    this.lockTime = lockTime;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * Function that give the Public Key of the Inputs
   * @return List<PublicKey> senders public keys
   */
  public List<PublicKey> getSendersTransaction() {
    Iterator<InputObject> inputIte = getInputs().iterator();
    List<PublicKey> senders = new ArrayList<>();

    //Through the inputs look at the sender
    while (inputIte.hasNext()) {
      senders.add(inputIte.next().getScript().getPubkey());
    }

    return senders;
  }

  /**
   * Function that give the Public Key Hash of the Outputs
   * @return List<String> outputs public keys hash
   */
  public List<String> getReceiversHashTransaction() {
    Iterator<OutputObject> outputIte = getOutputs().iterator();
    List<String> receiverHash = new ArrayList<>();

    while (outputIte.hasNext()) {
      receiverHash.add(outputIte.next().getScript().getPubkeyHash());
    }

    return receiverHash;
  }

  /**
   * Function that give the Public Key of the Outputs
   * @param mapHashKey Map<String,PublicKey> dictionary public key by public key hash
   * @return List<PublicKey> outputs public keys
   */
  public List<PublicKey> getReceiversTransaction(Map<String, PublicKey> mapHashKey) {
    Iterator<String> receiverHashIte = getReceiversHashTransaction().iterator();
    List<PublicKey> receivers = new ArrayList<>();
    while (receiverHashIte.hasNext()){
      PublicKey pub = mapHashKey.getOrDefault(receiverHashIte.next(),null);
      if (pub == null) {
        throw new IllegalArgumentException("The hash correspond to no key in the dictionary");
      }
      receivers.add(pub);
    }
    return receivers;
  }

  /**
   * Function that give the Public Key receiver and the amount on the Output of transaction
   *
   * @param mapHashKey Map<String,PublicKey> dictionary public key by public key hash
   * @return List<PublicKey,Long> outputs public keys
   */
  public Map<PublicKey, Long> getReceiversTransactionMap(Map<String, PublicKey> mapHashKey) {
    Iterator<String> receiverHashIte = getReceiversHashTransaction().iterator();
    Map<PublicKey, Long> receivers = new HashMap<>();
    while (receiverHashIte.hasNext()) {
      PublicKey pub = mapHashKey.getOrDefault(receiverHashIte.next(), null);
      if (pub == null) {
        throw new IllegalArgumentException("The hash correspond to no key in the dictionary");
      }
      receivers.putIfAbsent(pub, this.getMiniLaoPerReceiver(pub));
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
   * Function that given a key pair change the sig of an input considering all the outputs
   *
   * @param keyPair of one input sender
   * @return sig other all the outputs and inputs with the public key
   */
  public String computeSigOutputsInputs(KeyPair keyPair) throws GeneralSecurityException {
    // input #1: tx_out_hash Value //input #1: tx_out_index Value
    // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
    // TxOut #1: LaoCoin Value​​ //TxOut #1: script.type Value //TxOut #1: script.pubkey_hash Value
    // TxOut #2: LaoCoin Value​​ //TxOut #2: script.type Value //TxOut #2: script.pubkey_hash
    // Value...
    String[] sig = new String[inputs.size() * 2 + outputs.size() * 3];
    Iterator<InputObject> iteInput = inputs.iterator();
    Iterator<OutputObject> iteOutput = outputs.iterator();

    int index = 0;
    while (iteInput.hasNext()){
      InputObject current = iteInput.next();
      sig[index] = current.getTxOutHash();
      sig[index + 1] = String.valueOf(current.getTxOutIndex());
      index = index + 2;
    }

    while (iteOutput.hasNext()){
      OutputObject current = iteOutput.next();
      sig[index] = String.valueOf(current.getValue());
      sig[index + 1] = current.getScript().getType();
      sig[index + 2] = current.getScript().getPubkeyHash();
      index = index + 3;
    }
    return keyPair.sign(new Base64URLData(String.join("", sig))).getEncoded();
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
      throw new IllegalArgumentException("The public Key is not contained in the receiver public key");
    }
    // Set the return value to nothing
    long miniLao = 0;
    // Compute the hash of the public key
    String hashKey = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    Iterator<OutputObject> iterator = getOutputs().iterator();
    while (iterator.hasNext()) {
      OutputObject current = iterator.next();
      if (current.getScript().getPubkeyHash().equals(hashKey)) {
        miniLao = miniLao + current.getValue();
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
    Iterator<TransactionObject> transactionIte = transaction.iterator();
    long totalAmount = 0;
    while (transactionIte.hasNext()) {
      long current = transactionIte.next().getMiniLaoPerReceiver(receiver);
      totalAmount += current;
    }
    return totalAmount;
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
    // Set the return value to nothing
    long miniLao = 0;
    // Compute the hash of the public key
    String computeHash = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    Iterator<OutputObject> iterator = getOutputs().iterator();
    while (iterator.hasNext()) {
      OutputObject current = iterator.next();
      if (current.getScript().getPubkeyHash().equals(computeHash)) {
        // return after first occurrence
        return current.getValue();
      }
    }
    return miniLao;
  }

  /**
   * Function that return the index of the output for a given key in this Transaction
   *
   * @param publicKey PublicKey of an individual in Transaction output
   * @return int index in the transaction outputs
   */
  public int getIndexTransaction(PublicKey publicKey) {
    Iterator<OutputObject> outputObjectIterator = outputs.listIterator();
    String hashPubkey = publicKey.computeHash();
    int index = 0;
    while (outputObjectIterator.hasNext()) {
      OutputObject current = outputObjectIterator.next();
      if (current.getScript().getPubkeyHash().equals(hashPubkey)) {
        return index;
      }
      index = index + 1;
    }
    throw new IllegalArgumentException(
        "this public key is not contained in the output of this transaction");
  }

  public String computeId() {
    // Make a list all the string in the transaction
    List<String> stringList = new ArrayList<>();
    // Add them in lexicographic order

    // Inputs
    for (int i = 0; i < inputs.size(); i++) {
      InputObject currentTxin = inputs.get(i);
      // Script
      // PubKey
      stringList.add(currentTxin.getScript().getPubkey().getEncoded());
      // Sig
      stringList.add(currentTxin.getScript().getSig().toString());
      // Type
      stringList.add(currentTxin.getScript().getType());
      // TxOutHash
      stringList.add(currentTxin.getTxOutHash());
      // TxOutIndex
      stringList.add(String.valueOf(currentTxin.getTxOutIndex()));
    }

    // lock_time
    stringList.add(String.valueOf(lockTime));
    // Outputs
    for (int i = 0; i < outputs.size(); i++) {
      OutputObject currentTxout = outputs.get(i);
      // Script
      // PubKeyHash
      stringList.add(currentTxout.getScript().getPubkeyHash());
      // Type
      stringList.add(currentTxout.getScript().getType());
      // Value
      stringList.add(String.valueOf(currentTxout.getValue()));
    }
    // Version
    stringList.add(String.valueOf(version));

    String concat = "";
    for (int i = 0; i < stringList.size(); i++) {
      String toAdd = stringList.get(i);
      concat = concat.concat(toAdd.length() + toAdd);
    }

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(concat.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      Log.e(
          this.getClass().toString(),
          "Something is wrong with the hash calculation",
          new IllegalArgumentException("Error in the computation of the transaction id"));
      return "-1";
    }
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
      throw new Resources.NotFoundException();
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
        && getInputs().get(0).getTxOutHash().equals(Hash.hash("none"))
        && (getInputs().get(0).getTxOutIndex() == 0);
  }
}
