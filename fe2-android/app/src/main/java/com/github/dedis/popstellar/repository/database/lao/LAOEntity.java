package com.github.dedis.popstellar.repository.database.lao;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Lao;

@Entity(tableName = "laos")
public class LAOEntity {

  @ColumnInfo(name = "lao")
  @PrimaryKey
  @NonNull
  private Lao lao;

  public LAOEntity(@NonNull Lao lao) {
    this.lao = lao;
  }

  @NonNull
  public Lao getLao() {
    return lao;
  }

  public void setLao(@NonNull Lao lao) {
    this.lao = lao;
  }
}
