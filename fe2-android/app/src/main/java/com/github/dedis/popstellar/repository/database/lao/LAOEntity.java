package com.github.dedis.popstellar.repository.database.lao;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Lao;

@Entity(tableName = "laos")
public class LAOEntity {

  @PrimaryKey
  @ColumnInfo(name = "lao_id")
  @NonNull
  private String laoId;

  @ColumnInfo(name = "lao")
  @NonNull
  private Lao lao;

  public LAOEntity(@NonNull String laoId, @NonNull Lao lao) {
    this.laoId = laoId;
    this.lao = lao;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  @NonNull
  public Lao getLao() {
    return lao.copy();
  }

  public void setLao(@NonNull Lao lao) {
    this.lao = lao.copy();
  }
}
