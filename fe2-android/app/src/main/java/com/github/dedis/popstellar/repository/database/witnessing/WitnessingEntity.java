package com.github.dedis.popstellar.repository.database.witnessing;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

@Entity(
    tableName = "witness_signatures",
    primaryKeys = {"lao_id", "message_id", "signing_witness"})
public class WitnessingEntity {

  @ColumnInfo(name = "lao_id")
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "message_id")
  @NonNull
  private final MessageID messageID;

  @ColumnInfo(name = "signing_witness")
  @NonNull
  private final PublicKey signingWitness;

  public WitnessingEntity(
      @NonNull String laoId, @NonNull MessageID messageID, @NonNull PublicKey signingWitness) {
    this.laoId = laoId;
    this.messageID = messageID;
    this.signingWitness = signingWitness;
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
  public PublicKey getSigningWitness() {
    return signingWitness;
  }
}
