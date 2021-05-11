package com.github.dedis.student20_pop.model.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class MeetingAndModification {
  @Embedded public Meeting meeting;

  @Relation(parentColumn = "id", entityColumn = "identifier")
  public List<ModificationSignature> modificationSignature;
}
