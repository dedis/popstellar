package com.github.dedis.popstellar.repository.local.entities;

import androidx.room.*;

import java.util.List;

public class LAOEntityRelation {

  @Embedded public LAOEntity lao;

  @Relation(
      parentColumn = "channel",
      entityColumn = "publicKey",
      associateBy = @Junction(LAOWitnessCrossRefEntity.class))
  public List<PersonEntity> witness;

  @Relation(parentColumn = "id", entityColumn = "identifier")
  public List<ModificationSignatureEntity> modificationSignature;

  @Relation(parentColumn = "channel", entityColumn = "laoChannel", entity = MeetingEntity.class)
  public List<MeetingAndModification> meetings;

  @Relation(parentColumn = "channel", entityColumn = "laoChannel")
  public List<RollCallEntity> rollCalls;
}
