package com.github.dedis.popstellar.repository.database.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.objects.security.MessageID
import java.util.Objects

@Entity(tableName = "messages")
@Immutable
class MessageEntity(
    @field:PrimaryKey @field:ColumnInfo(name = "message_id") val messageId: MessageID,
    @field:ColumnInfo(name = "message") val content: MessageGeneral?
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as MessageEntity
    return messageId == that.messageId && content == that.content
  }

  override fun hashCode(): Int {
    return Objects.hash(messageId, content)
  }
}
