package com.github.dedis.student20_pop.model.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.entities.LAOWitnessCrossRef;
import com.github.dedis.student20_pop.model.entities.Meeting;
import com.github.dedis.student20_pop.model.entities.ModificationSignature;
import com.github.dedis.student20_pop.model.entities.Person;
import com.github.dedis.student20_pop.model.entities.RollCall;
import java.util.List;
import java.util.stream.Collectors;

@Dao
public abstract class LAODao {

  @Query("SELECT * from lao")
  public abstract List<LAO> getAll();

  @Transaction
  @Query("SELECT * from lao where channel LIKE :channel")
  public abstract LAOEntity getLAO(String channel);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void addLao(LAO lao);

  @Transaction
  public void updateLAO(LAO lao, List<Person> witnesses, List<ModificationSignature> signatures) {
    // update the LAO
    _updateLAO(lao);

    // update witness
    _updateWitness(lao, witnesses);

    // add modification signatures
    for (ModificationSignature signature : signatures) {
      signature.identifier = lao.id;
    }
    _addModificationSignature(signatures);
  }

  @Update
  public abstract void _updateLAO(LAO lao);

  public void _updateWitness(LAO lao, List<Person> witness) {
    // remove old witness references
    _deleteByLAOChannel(lao.channel);

    // add new ones
    _addWitness(witness);
    _addWitnessCrossRefs(
        witness.stream()
            .map(w -> new LAOWitnessCrossRef(lao.channel, w.publicKey))
            .collect(Collectors.toList()));
  }

  @Delete(entity = LAOWitnessCrossRef.class)
  public abstract void _deleteByLAOChannel(String channel);

  @Insert
  public abstract void _addWitnessCrossRefs(List<LAOWitnessCrossRef> witnessCrossRefs);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void _addWitness(List<Person> witness);

  public void addRollCall(LAO lao, RollCall rollCall) {
    rollCall.laoChannel = lao.channel;
    _addRollCall(rollCall);
  }

  @Insert
  abstract void _addRollCall(RollCall rollCall);

  @Update
  abstract void updateRollCall(RollCall rollCall);

  public void addMeeting(LAO lao, Meeting meeting) {
    meeting.laoChannel = lao.channel;
    _addMeeting(meeting);
  }

  @Insert
  abstract void _addMeeting(Meeting meeting);

  @Transaction
  public void updateMeeting(Meeting meeting, List<ModificationSignature> signatures) {
    // No need to remove the old signatures because the id is different after modification.
    for (ModificationSignature signature : signatures) {
      signature.identifier = meeting.id;
    }
    _updateMeeting(meeting);
    _addModificationSignature(signatures);
  }

  @Update
  abstract void _updateMeeting(Meeting meeting);

  @Insert
  abstract void _addModificationSignature(List<ModificationSignature> signatures);
}
