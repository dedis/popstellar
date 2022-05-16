package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

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

  // function that give the list of sender
  public List<PublicKey> get_senders_transaction() {
    return null;
  }

  // function that give the list of receiver_hash
  public List<String> get_receivers_hash_transaction() {
    return null;
  }

  // function that give the list of receiver
  public List<PublicKey> get_receivers_transaction(Map<String, PublicKey> map_hash_key) {
    return null;
  }

  // function that given a receiver get the miniLaoCoin he has
  public int get_miniLao_per_receiver(PublicKey receiver) {
    // first make the public key to the hash
    return 0;
  }

  // function that say if it was a coin base transaction

  // function that given a transaction check if the value send are alright
  // check also of the lock_time is after

  // function that given a list of attendees check if the transaction send is correct
}
