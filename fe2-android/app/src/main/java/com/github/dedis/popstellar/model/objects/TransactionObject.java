package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
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
   *
   * @return List<PublicKey> senders public keys
   */
  public List<PublicKey> getSendersTransaction() {
    Iterator<InputObject> input_ite = getInputs().iterator();
    List<PublicKey> senders= new ArrayList<>();

    //Through the inputs look at the sender
    while (input_ite.hasNext()){
      PublicKey current_sender = new PublicKey(input_ite.next().getScript().getPubkey());
      senders.add(current_sender);
    }

    return senders;
  }

  /**
   * Function that give the Public Key Hash of the Outputs
   *
   * @return List<String> outputs public keys hash
   */
  public List<String> getReceiversHashTransaction() {
    Iterator<OutputObject> output_ite = getOutputs().iterator();
    List<String> receiver_hash = new ArrayList<>();

    while(output_ite.hasNext()){
      receiver_hash.add(output_ite.next().getScript().getPubkeyHash());
    }

    return receiver_hash;
  }

  /**
   * Function that give the Public Key of the Outputs
   *
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
   * @throws GeneralSecurityException
   */
  public String computeSigOutputsInputs(KeyPair keyPair) throws GeneralSecurityException {
    // input #1: tx_out_hash Value //input #1: tx_out_index Value
    // input #2: tx_out_hash Value //input #2: tx_out_index Value ...
    // TxOut #1: LaoCoin Value​​ //TxOut #1: script.type Value //TxOut #1: script.pubkey_hash Value
    // TxOut #2: LaoCoin Value​​ //TxOut #2: script.type Value //TxOut #2: script.pubkey_hash
    // Value...
    String[] sig = new String[inputs.size() * 2 + outputs.size() * 3];
    Iterator<InputObject> ite_input = inputs.iterator();
    Iterator<OutputObject> ite_output = outputs.iterator();

    int index = 0;
    while (ite_input.hasNext()){
      InputObject current = ite_input.next();
      sig[index] = current.getTxOutHash();
      sig[index + 1] = String.valueOf(current.getTxOutIndex());
      index = index + 2;
    }

    while (ite_output.hasNext()){
      OutputObject current = ite_output.next();
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
      throw new IllegalArgumentException(
          "The public Key is not contained in the receiver public key");
    }
    // Set the return value to nothing
    long miniLao = 0;
    // Compute the hash of the public key
    String hash_key = receiver.computeHash();
    // iterate through the output and sum if it's for the argument public key
    Iterator<OutputObject> iterator = getOutputs().iterator();
    while (iterator.hasNext()) {
      OutputObject current = iterator.next();
      if (current.getScript().getPubkeyHash().equals(hash_key)) {
        miniLao = miniLao + current.getValue();
      }
    }
    return miniLao;
  }

  public String computeId() {
    // Make a list all the string in the transaction
    List<String> collect_transaction = new ArrayList<String>();
    // Add them in lexicographic order

    // Inputs
    for (int i = 0; i < inputs.size(); i++) {
      InputObject currentTxin = inputs.get(i);
      // Script
      // PubKey
      collect_transaction.add(currentTxin.getScript().getPubkey());
      // Sig
      collect_transaction.add(currentTxin.getScript().getSig());
      // Type
      collect_transaction.add(currentTxin.getScript().getType());
      // TxOutHash
      collect_transaction.add(currentTxin.getTxOutHash());
      // TxOutIndex
      collect_transaction.add(String.valueOf(currentTxin.getTxOutIndex()));
    }

    // lock_time
    collect_transaction.add(String.valueOf(lockTime));
    // Outputs
    for (int i = 0; i < outputs.size(); i++) {
      OutputObject currentTxout = outputs.get(i);
      // Script
      // PubKeyHash
      collect_transaction.add(currentTxout.getScript().getPubkeyHash());
      // Type
      collect_transaction.add(currentTxout.getScript().getType());
      // Value
      collect_transaction.add(String.valueOf(currentTxout.getValue()));
    }
    // Version
    collect_transaction.add(String.valueOf(version));

    String concat = "";
    for (int i = 0; i < collect_transaction.size(); i++) {
      String to_add = collect_transaction.get(i);
      concat = concat.concat(String.valueOf(to_add.length()) + to_add);
    }

    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(concat.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      System.err.println("Something is wrong");
      throw new IllegalArgumentException("Error in the computation of the transaction id");
    }
  }

  /**
   * Function that return the index of the output for a given key in this Transaction
   *
   * @param publicKey PublicKey of an individual in Transaction output
   * @return int index in the transaction outputs
   */
  public int getIndexTransaction(PublicKey publicKey) {
    Iterator<OutputObject> output_objectIterator = outputs.listIterator();
    String hash_pubkey = publicKey.computeHash();
    int index = 0;
    while (output_objectIterator.hasNext()) {
      OutputObject current = output_objectIterator.next();
      if (current.getScript().getPubkeyHash().equals(hash_pubkey)) {
        return index;
      }
      index = index + 1;
    }
    throw new IllegalArgumentException(
        "this public key is not contained in the output of this transaction");
  }



}
