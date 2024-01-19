package com.github.dedis.popstellar.repository.database.socialmedia

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.MessageID

@Entity(tableName = "reactions")
@Immutable
class ReactionEntity(
    @field:ColumnInfo(name = "reaction_id") @field:PrimaryKey val reactionId: MessageID,
    @field:ColumnInfo(name = "chirp_id", index = true) val chirpId: MessageID,
    @field:ColumnInfo(name = "reaction") val reaction: Reaction
) {

  @Ignore constructor(reaction: Reaction) : this(reaction.id, reaction.chirpId, reaction)
}
