package com.github.dedis.popstellar.repository.database.socialmedia

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.Chirp
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface ChirpDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(chirpEntity: ChirpEntity): Completable

  @Query("SELECT chirp FROM chirps WHERE lao_id = :laoId")
  fun getChirpsByLaoId(laoId: String): Single<List<Chirp>?>
}
