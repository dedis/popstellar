package com.github.dedis.popstellar.repository.database.event.rollcall;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.RollCall;

@Entity(tableName = "rollcalls")
@Immutable
public class RollCallEntity {

  @PrimaryKey
  @ColumnInfo(name = "rollcall_id")
  @NonNull
  private final String rollcallId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "rollcall")
  @NonNull
  private final RollCall rollCall;

  public RollCallEntity(
      @NonNull String rollcallId, @NonNull String laoId, @NonNull RollCall rollCall) {
    this.rollcallId = rollcallId;
    this.laoId = laoId;
    this.rollCall = rollCall;
  }

  @Ignore
  public RollCallEntity(@NonNull String laoId, @NonNull RollCall rollCall) {
    this(rollCall.getPersistentId(), laoId, rollCall);
  }

  @NonNull
  public String getRollcallId() {
    return rollcallId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public RollCall getRollCall() {
    return rollCall;
  }
}
