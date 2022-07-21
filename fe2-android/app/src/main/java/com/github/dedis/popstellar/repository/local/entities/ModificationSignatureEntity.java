package com.github.dedis.popstellar.repository.local.entities;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity
@SuppressWarnings("NotNullFieldNotInitialized")
public class ModificationSignatureEntity {

  @PrimaryKey @NonNull public String signature;

  public String witnessPublicKey;

  @NonNull
  @ColumnInfo(index = true)
  public String identifier;
}
