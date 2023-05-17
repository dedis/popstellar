package com.github.dedis.popstellar.repository.database.event.election;

import androidx.room.*;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ElectionDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(ElectionEntity electionEntity);

  /**
   * This function is a query execution to search for elections that match a given lao but have
   * their ids different from a specified set to exclude (this represents the elections already in
   * memory and useless to retrieve).
   *
   * @param laoId identifier of the lao where to search the elections
   * @param filteredIds ids of the election to exclude from the search
   * @return an emitter of a list of election entities (entries in the db)
   */
  @Query("SELECT * FROM elections WHERE lao_id = :laoId AND election_id NOT IN (:filteredIds)")
  Single<List<ElectionEntity>> getElectionsByLaoId(String laoId, Set<String> filteredIds);
}
