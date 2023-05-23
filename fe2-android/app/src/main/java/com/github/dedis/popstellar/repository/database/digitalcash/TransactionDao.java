package com.github.dedis.popstellar.repository.database.digitalcash;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface TransactionDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(TransactionEntity transactionEntity);

  @Query("SELECT `transaction` FROM transactions WHERE lao_id = :laoId")
  Single<List<TransactionObject>> getTransactionsByLaoId(String laoId);

  @Query("DELETE FROM transactions WHERE lao_id = :laoId")
  Completable deleteByLaoId(String laoId);
}
