package com.github.dedis.popstellar.model.entities;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import java.util.List;

public class LAOEntity {

  @Embedded
  public LAO lao;

  @Relation(
      parentColumn = "channel",
      entityColumn = "publicKey",
      associateBy = @Junction(LAOWitnessCrossRef.class))
  public List<Person> witness;

  @Relation(parentColumn = "id", entityColumn = "identifier")
  public List<ModificationSignature> modificationSignature;

  @Relation(parentColumn = "channel", entityColumn = "laoChannel", entity = Meeting.class)
  public List<MeetingAndModification> meetings;

  @Relation(parentColumn = "channel", entityColumn = "laoChannel")
  public List<RollCall> rollCalls;
}
