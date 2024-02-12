package com.github.dedis.popstellar.repository.database.digitalcash

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface HashDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(hashEntity: List<HashEntity>): Completable

  @Query("SELECT * FROM hash_dictionary WHERE lao_id = :laoId")
  fun getDictionaryByLaoId(laoId: String): Single<List<HashEntity>?>

  @Query("DELETE FROM hash_dictionary WHERE lao_id = :laoId")
  fun deleteByLaoId(laoId: String): Completable
}
