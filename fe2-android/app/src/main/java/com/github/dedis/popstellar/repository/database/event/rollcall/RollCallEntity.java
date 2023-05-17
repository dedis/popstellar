package com.github.dedis.popstellar.repository.database.event.rollcall;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.RollCall;

@Entity(tableName = "rollcalls")
public class RollCallEntity {

  @PrimaryKey
  @ColumnInfo(name = "rollcall_id")
  @NonNull
  private String rollcallId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private String laoId;

  @ColumnInfo(name = "rollcall")
  @NonNull
  private RollCall rollCall;

  public RollCallEntity(
      @NonNull String rollcallId, @NonNull String laoId, @NonNull RollCall rollCall) {
    this.rollcallId = rollcallId;
    this.laoId = laoId;
    this.rollCall = rollCall;
  }

  @NonNull
  public String getRollcallId() {
    return rollcallId;
  }

  public void setRollcallId(@NonNull String rollcallId) {
    this.rollcallId = rollcallId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  @NonNull
  public RollCall getRollCall() {
    return rollCall;
  }

  public void setRollCall(@NonNull RollCall rollCall) {
    this.rollCall = rollCall;
  }
}
