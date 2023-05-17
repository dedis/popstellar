package com.github.dedis.popstellar.repository.database.event.election;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Election;

@Entity(tableName = "elections")
public class ElectionEntity {

  @PrimaryKey
  @ColumnInfo(name = "election_id")
  @NonNull
  private String electionId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private String laoId;

  @ColumnInfo(name = "election")
  @NonNull
  private Election election;

  public ElectionEntity(
      @NonNull String electionId, @NonNull String laoId, @NonNull Election election) {
    this.electionId = electionId;
    this.laoId = laoId;
    this.election = election;
  }

  @NonNull
  public String getElectionId() {
    return electionId;
  }

  public void setElectionId(@NonNull String electionId) {
    this.electionId = electionId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  @NonNull
  public Election getElection() {
    return election;
  }

  public void setElection(@NonNull Election election) {
    this.election = election;
  }
}
