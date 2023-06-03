package com.github.dedis.popstellar.repository.database.event.meeting;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Meeting;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface MeetingDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(MeetingEntity meetingEntity);

  /**
   * This function is a query execution to search for meetings that are contained in a given lao.
   *
   * @param laoId identifier of the lao where to search the meetings
   * @return an emitter of a list of meetings
   */
  @Query("SELECT meeting FROM meetings WHERE lao_id = :laoId")
  Single<List<Meeting>> getMeetingsByLaoId(String laoId);
}
