package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public final class OpenElection extends Data {

  @SerializedName("lao")
  private final String laoId;

  @SerializedName("election")
  private final String electionId;

  @SerializedName("opened_at")
  private final long openedAt;

  public OpenElection(@NonNull String laoId, @NonNull String electionId, long openedAt) {
    this.laoId = laoId;
    this.electionId = electionId;
    this.openedAt = openedAt;
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.OPEN.getAction();
  }

  public String getLaoId() {
    return laoId;
  }

  public String getElectionId() {
    return electionId;
  }

  public long getOpenedAt() {
    return openedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenElection that = (OpenElection) o;

    return openedAt == that.openedAt
        && java.util.Objects.equals(laoId, that.laoId)
        && java.util.Objects.equals(electionId, that.electionId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(laoId, electionId, openedAt);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "OpenElection{lao='%s', election='%s', opened_at=%s}", laoId, electionId, openedAt);
  }
}
