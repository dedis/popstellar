package com.github.dedis.popstellar.repository.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class RollCallEntity {

  @PrimaryKey @NonNull public String id;

  public String laoChannel;

  public String name;

  public Long creation;

  public Long start;

  public Long scheduled;

  public String location;

  public String description;
}
