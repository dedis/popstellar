package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * "Match an ElectionKey query. This message is sent by the server"
 */
public class ElectionKey extends Data {

  @SerializedName("election")
  private String electionId;

  // Public key of the election for casting encrypted votes
  @SerializedName("election_key")
  private String electionKey;

  public ElectionKey(@NonNull String electionId, @NonNull String electionKey){
    this.electionId=electionId;
    this.electionKey=electionKey;
  }

  @NonNull
  public String getElectionKey(){
    return electionKey;
  }

  @NonNull
  public String getElectionId(){
    return electionId;
  }

  @Override
  public String getAction() {
    return Action.KEY.getAction();
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public boolean equals(Object o){
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionKey that = (ElectionKey) o;
    return electionKey.equals(that.getElectionKey()) && electionId.equals(that.getElectionId());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(electionId, electionKey);
  }

  @NonNull
  @Override
  public String toString(){
    return String.format(
        "ElectionKey{election='%s', election_key='%s'}", electionId, electionKey);
  }

}
