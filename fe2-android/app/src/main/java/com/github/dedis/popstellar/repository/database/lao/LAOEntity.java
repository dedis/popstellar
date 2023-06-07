package com.github.dedis.popstellar.repository.database.lao;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Lao;

@Entity(tableName = "laos")
@Immutable
public class LAOEntity {

  @PrimaryKey
  @ColumnInfo(name = "lao_id")
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "lao")
  @NonNull
  private final Lao lao;

  public LAOEntity(@NonNull String laoId, @NonNull Lao lao) {
    this.laoId = laoId;
    this.lao = lao;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public Lao getLao() {
    return lao.copy();
  }
}
