package com.github.dedis.popstellar.repository.database.witnessing

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.security.MessageID

@Entity(tableName = "pending_objects")
@Immutable
class PendingEntity {
  @PrimaryKey @ColumnInfo(name = "id") val messageID: MessageID

  @ColumnInfo(name = "lao_id") val laoId: String

  @ColumnInfo(name = "rollcall") val rollCall: RollCall?

  @ColumnInfo(name = "election") val election: Election?

  @ColumnInfo(name = "meeting") val meeting: Meeting?

  constructor(
      messageID: MessageID,
      laoId: String,
      rollCall: RollCall?,
      election: Election?,
      meeting: Meeting?
  ) {
    this.messageID = messageID
    this.laoId = laoId
    this.rollCall = rollCall
    this.election = election
    this.meeting = meeting
  }

  @Ignore
  constructor(messageID: MessageID, laoId: String, rollCall: RollCall) {
    this.messageID = messageID
    this.laoId = laoId
    this.rollCall = rollCall
    election = null
    meeting = null
  }

  @Ignore
  constructor(messageID: MessageID, laoId: String, election: Election) {
    this.messageID = messageID
    this.laoId = laoId
    rollCall = null
    this.election = election
    meeting = null
  }

  @Ignore
  constructor(messageID: MessageID, laoId: String, meeting: Meeting) {
    this.messageID = messageID
    this.laoId = laoId
    rollCall = null
    election = null
    this.meeting = meeting
  }

  val objectType: Objects?
    get() {
      if (rollCall != null) {
        return Objects.ROLL_CALL
      } else if (election != null) {
        return Objects.ELECTION
      } else if (meeting != null) {
        return Objects.MEETING
      }
      return null
    }
}
