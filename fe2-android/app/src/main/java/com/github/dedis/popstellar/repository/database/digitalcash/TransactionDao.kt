package com.github.dedis.popstellar.repository.database.digitalcash

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface TransactionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(transactionEntity: TransactionEntity): Completable

  @Query("SELECT `transaction` FROM transactions WHERE lao_id = :laoId")
  fun getTransactionsByLaoId(laoId: String): Single<List<TransactionObject>?>

  @Query("DELETE FROM transactions WHERE lao_id = :laoId")
  fun deleteByLaoId(laoId: String): Completable
}
