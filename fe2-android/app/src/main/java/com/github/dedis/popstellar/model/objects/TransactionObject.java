package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    // Empty constructor // empty transaction
    // change the sig for all the inputs
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
      PublicKey currentSender = new PublicKey(inputIte.next().getScript().getPubkey());
      senders.add(currentSender);
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
      receiverHash.add(outputIte.next().getPubKeyHash());
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
   * Check if a public key is in the recipient
   *
   * @param publicKey PublicKey of someone
   * @return true if public key in receiver, false otherwise
   */
  public boolean isReceiver(PublicKey publicKey) {
    return getReceiversHashTransaction().contains(publicKey.computeHash());
  }

  /**
   * Function that given a key pair change the sig of an input considering all the outputs
   *
   * @param keyPair of one input sender
   * @return sig other all the outputs and inputs with the public key
   * @throws GeneralSecurityException
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
      sig[index + 2] = current.getPubKeyHash();
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
  public int getMiniLaoPerReceiver(PublicKey receiver) {
    // Check in the future if useful
    if (!isReceiver(receiver)) {
      throw new IllegalArgumentException("The public Key is not contained in the receiver public key");
    }
    // Set the return value to nothing
    int miniLao = 0;
    // Compute the hash of the public key
    String hashKey = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    Iterator<OutputObject> iterator = getOutputs().iterator();
    while (iterator.hasNext()) {
      OutputObject current = iterator.next();
      if (current.getPubKeyHash().equals(hashKey)) {
        miniLao = miniLao + current.getValue();
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
      if (current.getPubKeyHash().equals(hashPubkey)) {
        return index;
      }
      index = index + 1;
    }
    throw new IllegalArgumentException(
        "this public key is not contained in the output of this transaction");
  }

}
