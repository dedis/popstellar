package com.github.dedis.popstellar.repository.database.core;

import androidx.room.*;

import io.reactivex.Completable;

/**
 * Interface to query the table containing the core information (server address, wallet seed and
 * subscriptions)
 */
@Dao
public interface CoreDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(CoreEntity coreEntity);

  @Query("SELECT * FROM core LIMIT 1")
  CoreEntity getSettings();
}
