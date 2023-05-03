package com.github.dedis.popstellar.repository.database.message;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.Objects;

import javax.annotation.Nullable;

@Entity(tableName = "messages")
public class MessageEntity {

  @ColumnInfo(name = "message_id")
  @PrimaryKey
  @NonNull
  private MessageID messageId;

  @ColumnInfo(name = "message")
  @Nullable
  private MessageGeneral content;

  public MessageEntity(@NonNull MessageID messageId, @Nullable MessageGeneral content) {
    this.messageId = messageId;
    this.content = content;
  }

  @NonNull
  public MessageID getMessageId() {
    return messageId;
  }

  public void setMessageId(@NonNull MessageID messageId) {
    this.messageId = messageId;
  }

  @Nullable
  public MessageGeneral getContent() {
    return content;
  }

  public void setContent(@Nullable MessageGeneral content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MessageEntity that = (MessageEntity) o;
    return messageId.equals(that.messageId) && Objects.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageId, content);
  }
}
