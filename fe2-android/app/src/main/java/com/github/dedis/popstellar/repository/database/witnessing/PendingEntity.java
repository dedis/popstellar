package com.github.dedis.popstellar.repository.database.witnessing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;

@Entity(tableName = "pending_objects")
@Immutable
public class PendingEntity {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @NonNull
  private final MessageID messageID;

  @ColumnInfo(name = "lao_id")
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "rollcall")
  @Nullable
  private final RollCall rollCall;

  @ColumnInfo(name = "election")
  @Nullable
  private final Election election;

  @ColumnInfo(name = "meeting")
  @Nullable
  private final Meeting meeting;

  public PendingEntity(
      @NonNull MessageID messageID,
      @NonNull String laoId,
      @NonNull RollCall rollCall,
      @NonNull Election election,
      @NonNull Meeting meeting) {
    this.messageID = messageID;
    this.laoId = laoId;
    this.rollCall = rollCall;
    this.election = election;
    this.meeting = meeting;
  }

  @Ignore
  public PendingEntity(
      @NonNull MessageID messageID, @NonNull String laoId, @NonNull RollCall rollCall) {
    this.messageID = messageID;
    this.laoId = laoId;
    this.rollCall = rollCall;
    this.election = null;
    this.meeting = null;
  }

  @Ignore
  public PendingEntity(
      @NonNull MessageID messageID, @NonNull String laoId, @NonNull Election election) {
    this.messageID = messageID;
    this.laoId = laoId;
    this.rollCall = null;
    this.election = election;
    this.meeting = null;
  }

  @Ignore
  public PendingEntity(
      @NonNull MessageID messageID, @NonNull String laoId, @NonNull Meeting meeting) {
    this.messageID = messageID;
    this.laoId = laoId;
    this.rollCall = null;
    this.election = null;
    this.meeting = meeting;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public MessageID getMessageID() {
    return messageID;
  }

  @Nullable
  public RollCall getRollCall() {
    return rollCall;
  }

  @Nullable
  public Election getElection() {
    return election;
  }

  @Nullable
  public Meeting getMeeting() {
    return meeting;
  }

  public Objects getObjectType() {
    if (rollCall != null) {
      return Objects.ROLL_CALL;
    } else if (election != null) {
      return Objects.ELECTION;
    } else if (meeting != null) {
      return Objects.MEETING;
    }
    return null;
  }
}
