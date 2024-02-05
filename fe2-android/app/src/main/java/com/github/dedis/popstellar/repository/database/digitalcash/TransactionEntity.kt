package com.github.dedis.popstellar.repository.database.digitalcash

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject

@Entity(tableName = "transactions")
@Immutable
class TransactionEntity(
    @field:ColumnInfo(name = "transaction_id") @field:PrimaryKey val transactionId: String,
    @field:ColumnInfo(name = "lao_id", index = true) val laoId: String,
    @field:ColumnInfo(name = "transaction") val transactionObject: TransactionObject
) {

  @Ignore
  constructor(
      laoId: String,
      transactionObject: TransactionObject
  ) : this(transactionObject.transactionId, laoId, transactionObject)
}
