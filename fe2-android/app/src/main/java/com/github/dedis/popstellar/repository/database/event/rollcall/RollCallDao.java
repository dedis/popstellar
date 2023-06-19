package com.github.dedis.popstellar.repository.database.event.rollcall;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.RollCall;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface RollCallDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(RollCallEntity rollCallEntity);

  /**
   * This function is a query execution to search for rollcalls in a given lao.
   *
   * @param laoId identifier of the lao where to search the rollcalls
   * @return an emitter of a list of rollcalls
   */
  @Query("SELECT rollcall FROM rollcalls WHERE lao_id = :laoId")
  Single<List<RollCall>> getRollCallsByLaoId(String laoId);
}
