package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

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
  private long lock_time;

  public Transaction_object() {
    // Empty constructor // empty transaction
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

  public long getLock_time() {
    return lock_time;
  }

  public void setLock_time(long lock_time) {
    this.lock_time = lock_time;
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
  public List<PublicKey> get_senders_transaction() {
    Iterator<Input_object> input_ite = getInputs().iterator();
    List<PublicKey> senders= new ArrayList<>();

    //Through the inputs look at the sender
    while (input_ite.hasNext()){
      PublicKey current_sender = new PublicKey(input_ite.next().get_script().get_pubkey());
      senders.add(current_sender);
    }

    return senders;
  }

  // function that give the list of receiver_hash

  /**
   * Function that give the Public Key Hash of the Outputs
   * @return List<String> outputs public keys hash
   */
  public List<String> get_receivers_hash_transaction() {
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
  public List<PublicKey> get_receivers_transaction(Map<String, PublicKey> map_hash_key) {
    Iterator<String> receiver_hash_ite = get_receivers_hash_transaction().iterator();
    List<PublicKey> receivers = new ArrayList<>();
    while (receiver_hash_ite.hasNext()){
      PublicKey pub = map_hash_key.getOrDefault(receiver_hash_ite.next(),new PublicKey("-1"));
      if (pub.equals(new PublicKey("-1"))) {
        throw new IllegalArgumentException("The hash correspond to no key in the dictionary");
      }
      receivers.add(pub);
    }
    return receivers;
  }

  public boolean is_receiver(PublicKey publicKey){
    get_receivers_hash_transaction().contains()
    return false;
  }

  // function that given a receiver get the miniLaoCoin he has
  public int get_miniLao_per_receiver(PublicKey receiver) {
    // first make the public key to the hash
    return 0;
  }

  // function that say if it was a coin base transaction
  public boolean is_this_coin_base_transaction() {
    return false;
  }

  // function that given a transaction check if the value send are alright
  // check also of the lock_time is after

  // function that given a list of attendees check if the transaction send is correct

  // function that sum two transaction for a given PublicKey
  public int transaction_add(List<Transaction_object> transaction_object_list, PublicKey receiver) {
    // transaction_object_list

    // get the hash of the public key

    // check that all the transaction have a output for the given public key

    return 0;
  }
}
