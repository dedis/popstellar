package com.github.dedis.popstellar.repository.database.witnessing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.security.PublicKey
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface WitnessDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(witnessEntities: List<WitnessEntity>): Completable

  @Query("SELECT witness FROM witnesses WHERE lao_id = :laoId")
  fun getWitnessesByLao(laoId: String): Single<List<PublicKey>>

  @Query("SELECT COUNT(*) FROM witnesses WHERE lao_id = :laoId AND witness = :witness")
  fun isWitness(laoId: String, witness: PublicKey): Int
}
