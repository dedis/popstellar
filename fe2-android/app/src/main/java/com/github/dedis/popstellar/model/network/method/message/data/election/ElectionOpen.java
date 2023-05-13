package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.google.gson.annotations.SerializedName;

@Immutable
public final class ElectionOpen extends Data {

  @NonNull
  @SerializedName("lao")
  private final String laoId;

  @NonNull
  @SerializedName("election")
  private final String electionId;

  @SerializedName("opened_at")
  private final long openedAt;

  /**
   * @param laoId id of the LAO
   * @param electionId id of the election
   * @param openedAt timestamp of election opening
   */
  public ElectionOpen(@NonNull String laoId, @NonNull String electionId, long openedAt) {
    // The election open handler checks that lao and election id match with an existing lao
    MessageValidator.verify()
        .isNotEmptyBase64(laoId, "lao id")
        .isNotEmptyBase64(electionId, "election id")
        .validPastTimes(openedAt);

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

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
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
    ElectionOpen election = (ElectionOpen) o;

    return openedAt == election.openedAt
        && laoId.equals(election.laoId)
        && electionId.equals(election.electionId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(laoId, electionId, openedAt);
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "ElectionOpen{lao='%s', election='%s', opened_at=%s}", laoId, electionId, openedAt);
  }
}
