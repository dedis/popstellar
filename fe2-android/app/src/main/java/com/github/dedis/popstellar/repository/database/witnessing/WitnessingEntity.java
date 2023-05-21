package com.github.dedis.popstellar.repository.database.witnessing;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.github.dedis.popstellar.model.objects.security.MessageID;

@Entity(
    tableName = "witness",
    primaryKeys = {"lao_id", "message_id"})
public class WitnessingEntity {

  @NonNull private final String laoId;

  @NonNull private final MessageID messageID;

  public WitnessingEntity(@NonNull String laoId, @NonNull MessageID messageID) {
    this.laoId = laoId;
    this.messageID = messageID;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public MessageID getMessageID() {
    return messageID;
  }
}
