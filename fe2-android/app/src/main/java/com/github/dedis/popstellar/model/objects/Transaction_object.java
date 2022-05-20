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

public class Transaction_object {
  private Channel channel;

  // version
  private int version;

  // inputs
  private List<Input_object> inputs;

  // outputs
  private List<Output_object> outputs;

  // lock_time
  private long lockTime;

  public Transaction_object() {
    // Empty constructor // empty transaction
    // change the sig for all the inputs
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(@NonNull Channel channel) {
    this.channel = channel;
  }

  public List<Input_object> getInputs() {
    return inputs;
  }

  public void setInputs(List<Input_object> inputs) {
    this.inputs = inputs;
  }

  public List<Output_object> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<Output_object> outputs) {
    this.outputs = outputs;
  }

  public long getLockTime() {
    return lockTime;
  }

  public void setLockTime(long lock_time) {
    this.lockTime = lock_time;
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
    Iterator<Input_object> input_ite = getInputs().iterator();
    List<PublicKey> senders= new ArrayList<>();

    //Through the inputs look at the sender
    while (input_ite.hasNext()){
      PublicKey current_sender = new PublicKey(input_ite.next().get_script().get_pubkey());
      senders.add(current_sender);
    }

    return senders;
  }

  /**
   * Function that give the Public Key Hash of the Outputs
   * @return List<String> outputs public keys hash
   */
  public List<String> getReceiversHashTransaction() {
    Iterator<Output_object> output_ite = getOutputs().iterator();
    List<String> receiver_hash = new ArrayList<>();

    while(output_ite.hasNext()){
      receiver_hash.add(output_ite.next().get_script().get_pubkey_hash());
    }

    return receiver_hash;
  }

  /**
   * Function that give the Public Key of the Outputs
   * @param map_hash_key Map<String,PublicKey> dictionary public key by public key hash
   * @return List<PublicKey> outputs public keys
   */
  public List<PublicKey> getReceiversTransaction(Map<String, PublicKey> map_hash_key) {
    Iterator<String> receiver_hash_ite = getReceiversHashTransaction().iterator();
    List<PublicKey> receivers = new ArrayList<>();
    while (receiver_hash_ite.hasNext()){
      PublicKey pub = map_hash_key.getOrDefault(receiver_hash_ite.next(),null);
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
    Iterator<Input_object> ite_input = inputs.iterator();
    Iterator<Output_object> ite_output = outputs.iterator();

    int index = 0;
    while (ite_input.hasNext()){
      Input_object current = ite_input.next();
      sig[index] = current.get_tx_out_hash();
      sig[index + 1] = String.valueOf(current.get_tx_out_index());
      index = index + 2;
    }

    while (ite_output.hasNext()){
      Output_object current = ite_output.next();
      sig[index] = String.valueOf(current.get_value());
      sig[index + 1] = current.get_script().get_type();
      sig[index + 2] = current.get_script().get_pubkey_hash();
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
    String hash_key = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    Iterator<Output_object> iterator = getOutputs().iterator();
    while (iterator.hasNext()) {
      Output_object current = iterator.next();
      if (current.get_script().get_pubkey_hash().equals(hash_key)) {
        miniLao = miniLao + current.get_value();
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
    Iterator<Output_object> output_objectIterator = outputs.listIterator();
    String hash_pubkey = publicKey.computeHash();
    int index = 0;
    while (output_objectIterator.hasNext()) {
      Output_object current = output_objectIterator.next();
      if (current.get_script().get_pubkey_hash().equals(hash_pubkey)) {
        return index;
      }
      index = index + 1;
    }
    throw new IllegalArgumentException(
        "this public key is not contained in the output of this transaction");
  }

}
