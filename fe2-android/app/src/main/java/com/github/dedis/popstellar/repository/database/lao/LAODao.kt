package com.github.dedis.popstellar.repository.database.lao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.Lao
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface LAODao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(laoEntity: LAOEntity): Completable

  @get:Query("SELECT lao FROM laos") val allLaos: Single<List<Lao>?>

  @Query("SELECT lao FROM laos WHERE lao_id = :laoId") fun getLaoById(laoId: String): Lao?
}
