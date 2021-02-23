package com.github.dedis.student20_pop.model.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"channel", "publicKey"})
public class LAOWitnessCrossRef {

  public LAOWitnessCrossRef(String channel, String publicKey) {
    this.channel = channel;
    this.publicKey = publicKey;
  }

  @NonNull public String channel;

  @NonNull
  @ColumnInfo(index = true)
  public String publicKey;
}
