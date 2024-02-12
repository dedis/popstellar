package com.github.dedis.popstellar.repository.database.witnessing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.MessageID
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface WitnessingDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(witnessingEntity: WitnessingEntity): Completable

  @Query("SELECT message FROM witness_messages WHERE lao_id = :laoId")
  fun getWitnessMessagesByLao(laoId: String): Single<List<WitnessMessage>?>

  @Query("DELETE FROM witness_messages WHERE lao_id = :laoId AND id IN (:filteredIds)")
  fun deleteMessagesByIds(laoId: String, filteredIds: Set<MessageID>): Completable
}
