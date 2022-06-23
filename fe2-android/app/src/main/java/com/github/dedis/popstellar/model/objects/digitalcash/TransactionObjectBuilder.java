package com.github.dedis.popstellar.model.objects.digitalcash;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.InputObject;
import com.github.dedis.popstellar.model.objects.OutputObject;

import java.util.List;

public class TransactionObjectBuilder {

  public TransactionObjectBuilder() {}

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

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public void setInputs(List<InputObject> inputs) {
    this.inputs = inputs;
  }

  public void setOutputs(List<OutputObject> outputs) {
    this.outputs = outputs;
  }

  public void setLockTime(long lockTime) {
    this.lockTime = lockTime;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public TransactionObject build() {
    if (channel == null || inputs == null || outputs == null || transactionId == null) {
      throw new IllegalStateException("One of the field is null");
    }
    return new TransactionObject(channel, version, inputs, outputs, lockTime, transactionId);
  }
}
