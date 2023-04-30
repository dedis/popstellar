package com.github.dedis.popstellar.repository.database.message;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import javax.annotation.Nullable;

@Entity(tableName = "messages")
public class MessageEntity {

  private static final MessageEntity EMPTY = new MessageEntity(new MessageID(""), null);

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

  public static MessageEntity getEmptyEntity() {
    return EMPTY;
  }
}
