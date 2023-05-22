package com.github.dedis.popstellar.repository.database.witnessing;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;

@Entity(tableName = "witness_messages")
public class WitnessingEntity {

  @ColumnInfo(name = "lao_id")
  @NonNull
  private final String laoId;

  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  private final MessageID messageID;

  @ColumnInfo(name = "message")
  @NonNull
  private final WitnessMessage message;

  public WitnessingEntity(
      @NonNull String laoId, @NonNull MessageID messageID, @NonNull WitnessMessage message) {
    this.laoId = laoId;
    this.messageID = messageID;
    this.message = message;
  }

  @Ignore
  public WitnessingEntity(@NonNull String laoId, @NonNull WitnessMessage message) {
    this(laoId, message.getMessageId(), message);
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public MessageID getMessageID() {
    return messageID;
  }

  @NonNull
  public WitnessMessage getMessage() {
    return message;
  }
}
