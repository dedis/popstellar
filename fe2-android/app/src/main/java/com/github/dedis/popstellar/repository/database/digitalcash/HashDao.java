package com.github.dedis.popstellar.repository.database.digitalcash;

import androidx.room.*;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface HashDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(List<HashEntity> hashEntity);

  @Query("SELECT * FROM hash_dictionary WHERE lao_id = :laoId")
  Single<List<HashEntity>> getDictionaryByLaoId(String laoId);

  @Query("DELETE FROM hash_dictionary WHERE lao_id = :laoId")
  Completable deleteByLaoId(String laoId);
}
