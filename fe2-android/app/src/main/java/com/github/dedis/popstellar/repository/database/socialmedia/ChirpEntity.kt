package com.github.dedis.popstellar.repository.database.socialmedia

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.security.MessageID

@Entity(tableName = "chirps")
@Immutable
class ChirpEntity(
    @field:ColumnInfo(name = "chirp_id") @field:PrimaryKey val chirpId: MessageID,
    @field:ColumnInfo(name = "lao_id", index = true) val laoId: String,
    @field:ColumnInfo(name = "chirp") val chirp: Chirp
) {

  @Ignore constructor(laoId: String, chirp: Chirp) : this(chirp.id, laoId, chirp)
}
