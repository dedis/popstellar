package com.github.dedis.popstellar.repository.local;

import com.github.dedis.popstellar.repository.LAODataSource.Local;
import com.github.dedis.popstellar.repository.local.dao.LAODao;
import com.github.dedis.popstellar.repository.local.entities.LAOEntity;
import com.github.dedis.popstellar.repository.local.entities.LAOEntityRelation;
import com.github.dedis.popstellar.repository.local.entities.MeetingEntity;
import com.github.dedis.popstellar.repository.local.entities.ModificationSignatureEntity;
import com.github.dedis.popstellar.repository.local.entities.PersonEntity;
import com.github.dedis.popstellar.repository.local.entities.RollCallEntity;

import java.util.List;

public class LAOLocalDataSource implements Local {

  private LAODao laoDao;

  private static LAOLocalDataSource INSTANCE;

  private LAOLocalDataSource(LAODatabase database) {
    laoDao = database.laoDao();
  }

  public static LAOLocalDataSource getInstance(LAODatabase database) {
    if (INSTANCE == null) {
      INSTANCE = new LAOLocalDataSource(database);
    }
    return INSTANCE;
  }

  @Override
  public List<LAOEntity> getAll() {
    return null;
  }

  @Override
  public LAOEntityRelation getLAO(String channel) {
    return laoDao.getLAO(channel);
  }

  @Override
  public void addLao(LAOEntity lao) {
    laoDao.addLao(lao);
  }

  @Override
  public void updateLAO(
      LAOEntity lao, List<PersonEntity> witnesses, List<ModificationSignatureEntity> signatures) {}

  @Override
  public void addRollCall(LAOEntity lao, RollCallEntity rollCall) {}

  @Override
  public void updateRollCall(RollCallEntity rollCall) {}

  @Override
  public void addMeeting(LAOEntity lao, MeetingEntity meeting) {}

  @Override
  public void updateMeeting(MeetingEntity meeting, List<ModificationSignatureEntity> signatures) {}
}
