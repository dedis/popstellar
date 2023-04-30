package com.github.dedis.popstellar.repository.database.core;

import androidx.room.*;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface CoreDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(CoreEntity coreEntity);

  @Query("SELECT * FROM core LIMIT 1")
  Single<CoreEntity> getSettings();
}
