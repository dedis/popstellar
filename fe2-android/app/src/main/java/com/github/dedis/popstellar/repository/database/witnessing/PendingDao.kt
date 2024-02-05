package com.github.dedis.popstellar.repository.database.witnessing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.security.MessageID
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface PendingDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(pendingEntity: PendingEntity): Completable

  @Query("SELECT * FROM pending_objects WHERE lao_id = :laoId")
  fun getPendingObjectsFromLao(laoId: String): Single<List<PendingEntity>?>

  @Query("DELETE FROM pending_objects WHERE id = :messageID")
  fun removePendingObject(messageID: MessageID): Completable
}
