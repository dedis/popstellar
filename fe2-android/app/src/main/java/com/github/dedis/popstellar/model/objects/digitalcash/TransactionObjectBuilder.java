package com.github.dedis.popstellar.model.objects.digitalcash;

import com.github.dedis.popstellar.model.objects.*;

import java.util.List;

public class TransactionObjectBuilder {

  private Channel channel;

  // version
  private int version;

  // inputs
  private List<InputObject> inputs;

  // outputs
  private List<OutputObject> outputs;

  // lock_time
  private long lockTime;

  private String transactionId;

  public TransactionObjectBuilder setChannel(Channel channel) {
    this.channel = channel;
    return this;
  }

  public TransactionObjectBuilder setVersion(int version) {
    this.version = version;
    return this;
  }

  public TransactionObjectBuilder setInputs(List<InputObject> inputs) {
    this.inputs = inputs;
    return this;
  }

  public TransactionObjectBuilder setOutputs(List<OutputObject> outputs) {
    this.outputs = outputs;
    return this;
  }

  public TransactionObjectBuilder setLockTime(long lockTime) {
    this.lockTime = lockTime;
    return this;
  }

  public TransactionObjectBuilder setTransactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public TransactionObject build() {
    if (channel == null) {
      throw new IllegalStateException("channel is null");
    }
    if (inputs == null) {
      throw new IllegalStateException("inputs is null");
    }
    if (outputs == null) {
      throw new IllegalStateException("outputs is null");
    }
    if (transactionId == null) {
      throw new IllegalStateException("transactionId is null");
    }
    return new TransactionObject(channel, version, inputs, outputs, lockTime, transactionId);
  }
}
