package com.github.dedis.popstellar.repository.database.event.election

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.Election
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface ElectionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(electionEntity: ElectionEntity): Completable

  /**
   * This function is a query execution to search for elections that are contained in a given lao.
   *
   * @param laoId identifier of the lao where to search the elections
   * @return an emitter of a list of elections
   */
  @Query("SELECT election FROM elections WHERE lao_id = :laoId")
  fun getElectionsByLaoId(laoId: String): Single<List<Election>?>
}
