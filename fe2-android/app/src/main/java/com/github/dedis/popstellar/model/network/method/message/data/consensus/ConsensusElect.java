package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.ElectInstance;
import com.google.gson.annotations.SerializedName;

public final class ConsensusElect extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("created_at")
  private final long creation;

  private final ConsensusKey key;

  private final Object value;

  public ConsensusElect(long creation, String objId, String type, String property, Object value) {
    this.instanceId = ElectInstance.generateConsensusId(type, objId, property);
    this.creation = creation;
    this.key = new ConsensusKey(type, objId, property);
    this.value = value;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public long getCreation() {
    return creation;
  }

  public ConsensusKey getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String getObject() {
    return Objects.CONSENSUS.getObject();
  }

  @Override
  public String getAction() {
    return Action.ELECT.getAction();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, creation, key, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsensusElect that = (ConsensusElect) o;

    return creation == that.creation
        && java.util.Objects.equals(instanceId, that.instanceId)
        && java.util.Objects.equals(key, that.key)
        && java.util.Objects.equals(value, that.value);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "ConsensusElect{instance_id='%s', created_at=%s, key='%s', value='%s'}",
        instanceId, creation, key, value);
  }
}
