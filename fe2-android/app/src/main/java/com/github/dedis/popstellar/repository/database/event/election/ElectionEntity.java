package com.github.dedis.popstellar.repository.database.event.election;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Election;

@Entity(tableName = "elections")
@Immutable
public class ElectionEntity {

  @PrimaryKey
  @ColumnInfo(name = "election_id")
  @NonNull
  private final String electionId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "election")
  @NonNull
  private final Election election;

  public ElectionEntity(
      @NonNull String electionId, @NonNull String laoId, @NonNull Election election) {
    this.electionId = electionId;
    this.laoId = laoId;
    this.election = election;
  }

  // Ignore the constructor for Room
  @Ignore
  public ElectionEntity(@NonNull Election election) {
    this(election.getId(), election.getChannel().extractLaoId(), election);
  }

  @NonNull
  public String getElectionId() {
    return electionId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public Election getElection() {
    return election;
  }
}
