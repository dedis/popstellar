package com.github.dedis.popstellar.repository.database.witnessing

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.MessageID

@Entity(tableName = "witness_messages")
class WitnessingEntity(
    @field:ColumnInfo(name = "lao_id") val laoId: String,
    @field:ColumnInfo(name = "id") @field:PrimaryKey val messageID: MessageID,
    @field:ColumnInfo(name = "message") val message: WitnessMessage
) {

  @Ignore
  constructor(laoId: String, message: WitnessMessage) : this(laoId, message.messageId, message)
}
