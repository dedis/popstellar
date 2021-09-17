package com.github.dedis.popstellar.model.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LAO {

  @PrimaryKey
  @NonNull
  public String channel;

  public String id;

  public String name;

  public Long lastModifiedAt;

  public Long createdAt;

  public String organizer;

  public String modificationId;
}
