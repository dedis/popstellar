package com.github.dedis.popstellar.repository.database.witnessing

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.github.dedis.popstellar.model.objects.security.PublicKey

@Entity(tableName = "witnesses", primaryKeys = ["lao_id", "witness"])
class WitnessEntity(
    @field:ColumnInfo(name = "lao_id") val laoId: String,
    @field:ColumnInfo(name = "witness") val witness: PublicKey
)
