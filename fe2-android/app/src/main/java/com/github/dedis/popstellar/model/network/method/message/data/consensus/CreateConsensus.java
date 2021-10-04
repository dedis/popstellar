package com.github.dedis.popstellar.model.network.method.message.data.consensus;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.google.gson.annotations.SerializedName;

public class CreateConsensus extends Data {

  @SerializedName("instance_id")
  private final String instanceId;

  @SerializedName("created_at")
  private final long creation;

  private final String objId;
  private final String type;
  private final String property;

  private final Object value;

  public CreateConsensus(long creation, String objId, String type, String property, Object value) {
    this.instanceId =
        Consensus.generateConsensusId(creation, type, objId, property, String.valueOf(value));
    this.creation = creation;

    this.objId = objId;
    this.type = type;
    this.property = property;
    this.value = value;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public long getCreation() {
    return creation;
  }

  public String getObjId() {
    return objId;
  }

  public String getType() {
    return type;
  }

  public String getProperty() {
    return property;
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
    return Action.PHASE_1_ELECT.getAction();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(instanceId, creation, objId, type, property, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateConsensus that = (CreateConsensus) o;

    return instanceId.equals(that.instanceId)
        && creation == that.creation
        && objId.equals(that.objId)
        && type.equals(that.type)
        && property.equals(that.property)
        && java.util.Objects.equals(value, that.value);
  }
}
