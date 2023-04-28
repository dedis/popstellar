package com.github.dedis.popstellar.repository.database.message;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;

@Entity(tableName = "messages")
public class MessageEntity {

  @ColumnInfo(name = "message_id")
  @PrimaryKey
  @NonNull
  private MessageID messageId;

  @ColumnInfo(name = "message")
  @NonNull
  private MessageGeneral content;

  public MessageEntity(@NonNull MessageID messageId, @NonNull MessageGeneral content) {
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

  @NonNull
  public MessageGeneral getContent() {
    return content;
  }

  public void setContent(@NonNull MessageGeneral content) {
    this.content = content;
  }
}
