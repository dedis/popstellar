package com.github.dedis.popstellar.repository.database.event.election;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Election;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ElectionDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(ElectionEntity electionEntity);

  /**
   * This function is a query execution to search for elections that are contained in a given lao.
   *
   * @param laoId identifier of the lao where to search the elections
   * @return an emitter of a list of elections
   */
  @Query("SELECT election FROM elections WHERE lao_id = :laoId")
  Single<List<Election>> getElectionsByLaoId(String laoId);
}
