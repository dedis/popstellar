package com.github.dedis.popstellar.repository.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ModificationSignatureEntity {

  @PrimaryKey @NonNull public String signature;

  public String witnessPublicKey;

  @NonNull
  @ColumnInfo(index = true)
  public String identifier;
}
