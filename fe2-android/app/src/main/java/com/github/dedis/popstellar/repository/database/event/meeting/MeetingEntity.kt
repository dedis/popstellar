package com.github.dedis.popstellar.repository.database.event.meeting

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Meeting

@Entity(tableName = "meetings")
@Immutable
class MeetingEntity(
    @field:ColumnInfo(name = "meeting_id") @field:PrimaryKey val meetingId: String,
    @field:ColumnInfo(name = "lao_id", index = true) val laoId: String,
    @field:ColumnInfo(name = "meeting") val meeting: Meeting
) {

  @Ignore constructor(laoId: String, meeting: Meeting) : this(meeting.id, laoId, meeting)
}
