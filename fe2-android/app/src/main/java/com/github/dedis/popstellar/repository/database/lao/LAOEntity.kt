package com.github.dedis.popstellar.repository.database.lao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Lao

@Entity(tableName = "laos")
@Immutable
class LAOEntity(
    @JvmField @field:ColumnInfo(name = "lao_id") @field:PrimaryKey val laoId: String,
    @field:ColumnInfo(name = "lao") private val lao: Lao
) {

  fun getLao(): Lao {
    return lao.copy()
  }
}
