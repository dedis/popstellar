package com.github.dedis.popstellar.repository.database.witnessing;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

@Entity(
    tableName = "witnesses",
    primaryKeys = {"lao_id", "witness"})
public class WitnessEntity {

  @ColumnInfo(name = "lao_id")
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "witness")
  @NonNull
  private final PublicKey witness;

  public WitnessEntity(@NonNull String laoId, @NonNull PublicKey witness) {
    this.laoId = laoId;
    this.witness = witness;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public PublicKey getWitness() {
    return witness;
  }
}
