package com.github.dedis.popstellar.repository.database.event.meeting;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Meeting;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface MeetingDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(MeetingEntity meetingEntity);

  /**
   * This function is a query execution to search for meetings that match a given lao but have their
   * ids different from a specified set to exclude (this represents the meetings already in memory
   * and useless to retrieve).
   *
   * @param laoId identifier of the lao where to search the meetings
   * @param filteredIds ids of the meetings to exclude from the search
   * @return an emitter of a list of meetings
   */
  @Query("SELECT meeting FROM meetings WHERE lao_id = :laoId AND meeting_id NOT IN (:filteredIds)")
  Single<List<Meeting>> getMeetingsByLaoId(String laoId, Set<String> filteredIds);
}
