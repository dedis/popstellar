package com.github.dedis.popstellar.repository.database.socialmedia;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ChirpDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(ChirpEntity chirpEntity);

  @Query("SELECT chirp FROM chirps WHERE lao_id = :laoId AND chirp_id NOT IN (:filteredIds)")
  Single<List<Chirp>> getChirpsByLaoId(String laoId, Set<MessageID> filteredIds);
}
