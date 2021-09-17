package com.github.dedis.popstellar.model.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Meeting {

  @PrimaryKey
  @NonNull
  public String originalId;

  @ColumnInfo(index = true)
  public String id;

  @ColumnInfo(index = true)
  public String laoChannel;

  public String name;

  public String creation;

  public String location;

  public Long start;

  public Long end;

  public String extra;

  public String modificationId;
}
