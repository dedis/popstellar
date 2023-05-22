package com.github.dedis.popstellar.repository.database.event.meeting;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Meeting;

@Entity(tableName = "meetings")
@Immutable
public class MeetingEntity {

  @PrimaryKey
  @ColumnInfo(name = "meeting_id")
  @NonNull
  private final String meetingId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "meeting")
  @NonNull
  private final Meeting meeting;

  public MeetingEntity(@NonNull String meetingId, @NonNull String laoId, @NonNull Meeting meeting) {
    this.meetingId = meetingId;
    this.laoId = laoId;
    this.meeting = meeting;
  }

  @Ignore
  public MeetingEntity(@NonNull String laoId, @NonNull Meeting meeting) {
    this(meeting.getId(), laoId, meeting);
  }

  @NonNull
  public String getMeetingId() {
    return meetingId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public Meeting getMeeting() {
    return meeting;
  }
}
