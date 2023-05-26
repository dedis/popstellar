package com.github.dedis.popstellar.repository.database.witnessing;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface WitnessDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(List<WitnessEntity> witnessEntities);

  @Query("SELECT witness FROM witnesses WHERE lao_id = :laoId")
  Single<List<PublicKey>> getWitnessesByLao(String laoId);

  @Query("SELECT COUNT(*) FROM witnesses WHERE lao_id = :laoId AND witness = :witness")
  int isWitness(String laoId, PublicKey witness);
}
