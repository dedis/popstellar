package com.github.dedis.popstellar.repository.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"channel", "publicKey"})
public class LAOWitnessCrossRefEntity {

  public LAOWitnessCrossRefEntity(@NonNull String channel, @NonNull String publicKey) {
    this.channel = channel;
    this.publicKey = publicKey;
  }

  @NonNull public final String channel;

  @NonNull
  @ColumnInfo(index = true)
  public final String publicKey;
}
