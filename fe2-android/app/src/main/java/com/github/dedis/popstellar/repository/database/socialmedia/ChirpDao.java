package com.github.dedis.popstellar.repository.database.socialmedia;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Chirp;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ChirpDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(ChirpEntity chirpEntity);

  @Query("SELECT chirp FROM chirps WHERE lao_id = :laoId")
  Single<List<Chirp>> getChirpsByLaoId(String laoId);
}
