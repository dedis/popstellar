package com.github.dedis.popstellar.repository.database.event.rollcall;

import androidx.room.*;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface RollCallDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(RollCallEntity electionEntity);

  /**
   * This function is a query execution to search for rollcalls that match a given lao but have
   * their ids different from a specified set to exclude (this represents the rollcalls already in
   * memory and useless to retrieve).
   *
   * @param laoId identifier of the lao where to search the rollcalls
   * @param filteredIds ids of the rollcall to exclude from the search
   * @return an emitter of a list of rollcall entities (entries in the db)
   */
  @Query("SELECT * FROM rollcalls WHERE lao_id = :laoId AND rollcall_id NOT IN (:filteredIds)")
  Single<List<RollCallEntity>> getRollCallsByLaoId(String laoId, Set<String> filteredIds);
}
