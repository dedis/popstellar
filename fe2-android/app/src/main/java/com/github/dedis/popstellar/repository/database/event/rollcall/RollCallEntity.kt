package com.github.dedis.popstellar.repository.database.event.rollcall

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.RollCall

@Entity(tableName = "rollcalls")
@Immutable
class RollCallEntity(
    @field:ColumnInfo(name = "rollcall_id") @field:PrimaryKey val rollcallId: String,
    @field:ColumnInfo(name = "lao_id", index = true) val laoId: String,
    @field:ColumnInfo(name = "rollcall") val rollCall: RollCall
) {

  @Ignore
  constructor(laoId: String, rollCall: RollCall) : this(rollCall.persistentId, laoId, rollCall)
}
