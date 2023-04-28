package com.github.dedis.popstellar.repository.database.lao;

import androidx.room.*;

import java.util.List;

import io.reactivex.Completable;

@Dao
public interface LAODao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(LAOEntity laoEntity);

  @Query("SELECT * FROM laos")
  List<LAOEntity> getAllLaos();
}
