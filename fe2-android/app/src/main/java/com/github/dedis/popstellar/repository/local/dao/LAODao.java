package com.github.dedis.popstellar.repository.local.dao;

import androidx.room.*;

import com.github.dedis.popstellar.repository.local.entities.*;

import java.util.List;
import java.util.stream.Collectors;

/** LAODao represents a Data Access Object, it defines the database interactions. */
@Dao
public abstract class LAODao {

  @Query("SELECT * from LAOEntity")
  public abstract List<LAOEntity> getAll();

  @Transaction
  @Query("SELECT * from LAOEntity where channel LIKE :channel")
  public abstract LAOEntityRelation getLAO(String channel);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void addLao(LAOEntity lao);

  @Transaction
  public void updateLAO(
      LAOEntity lao, List<PersonEntity> witnesses, List<ModificationSignatureEntity> signatures) {
    // update the LAO
    _updateLAO(lao);

    // update witness
    _updateWitness(lao, witnesses);

    // add modification signatures
    for (ModificationSignatureEntity signature : signatures) {
      signature.identifier = lao.id;
    }
    _addModificationSignature(signatures);
  }

  @Update
  public abstract void _updateLAO(LAOEntity lao);

  public void _updateWitness(LAOEntity lao, List<PersonEntity> witness) {
    // remove old witness references
    _deleteByLAOChannel(new Channel(lao.channel));

    // add new ones
    _addWitness(witness);
    _addWitnessCrossRefs(
        witness.stream()
            .map(w -> new LAOWitnessCrossRefEntity(lao.channel, w.publicKey))
            .collect(Collectors.toList()));
  }

  @Delete(entity = LAOWitnessCrossRefEntity.class)
  public abstract void _deleteByLAOChannel(Channel channel);

  @Insert
  public abstract void _addWitnessCrossRefs(List<LAOWitnessCrossRefEntity> witnessCrossRefs);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void _addWitness(List<PersonEntity> witness);

  public void addRollCall(LAOEntity lao, RollCallEntity rollCall) {
    rollCall.laoChannel = lao.channel;
    _addRollCall(rollCall);
  }

  @Insert
  abstract void _addRollCall(RollCallEntity rollCall);

  @Update
  abstract void updateRollCall(RollCallEntity rollCall);

  public void addMeeting(LAOEntity lao, MeetingEntity meeting) {
    meeting.laoChannel = lao.channel;
    _addMeeting(meeting);
  }

  @Insert
  abstract void _addMeeting(MeetingEntity meeting);

  @Transaction
  public void updateMeeting(MeetingEntity meeting, List<ModificationSignatureEntity> signatures) {
    // No need to remove the old signatures because the id is different after modification.
    for (ModificationSignatureEntity signature : signatures) {
      signature.identifier = meeting.id;
    }
    _updateMeeting(meeting);
    _addModificationSignature(signatures);
  }

  @Update
  abstract void _updateMeeting(MeetingEntity meeting);

  @Insert
  abstract void _addModificationSignature(List<ModificationSignatureEntity> signatures);
}
