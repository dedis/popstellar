package com.github.dedis.popstellar.repository.database.event.meeting

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.Meeting
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface MeetingDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(meetingEntity: MeetingEntity): Completable

  /**
   * This function is a query execution to search for meetings that are contained in a given lao.
   *
   * @param laoId identifier of the lao where to search the meetings
   * @return an emitter of a list of meetings
   */
  @Query("SELECT meeting FROM meetings WHERE lao_id = :laoId")
  fun getMeetingsByLaoId(laoId: String): Single<List<Meeting>?>
}
