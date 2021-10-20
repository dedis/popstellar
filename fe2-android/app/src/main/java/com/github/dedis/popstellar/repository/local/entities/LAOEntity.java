package com.github.dedis.popstellar.repository.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LAOEntity {

  @PrimaryKey @NonNull public String channel;

  public String id;

  public String name;

  public Long lastModifiedAt;

  public Long createdAt;

  public String organizer;

  public String modificationId;
}
