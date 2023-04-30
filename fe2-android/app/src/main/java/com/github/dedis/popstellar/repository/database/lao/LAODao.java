package com.github.dedis.popstellar.repository.database.lao;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Lao;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface LAODao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(LAOEntity laoEntity);

  @Query("SELECT lao FROM laos")
  Single<List<Lao>> getAllLaos();
}
