package com.github.dedis.popstellar.repository.database.event.election

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Election

@Entity(tableName = "elections")
@Immutable
class ElectionEntity(
    @field:ColumnInfo(name = "election_id") @field:PrimaryKey val electionId: String,
    @field:ColumnInfo(name = "lao_id", index = true) val laoId: String,
    @field:ColumnInfo(name = "election") val election: Election
) {

  // Ignore the constructor for Room
  @Ignore
  constructor(election: Election) : this(election.id, election.channel.extractLaoId(), election)
}
