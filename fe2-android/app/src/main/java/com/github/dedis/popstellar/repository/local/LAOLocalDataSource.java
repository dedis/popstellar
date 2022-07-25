package com.github.dedis.popstellar.repository.local;

import com.github.dedis.popstellar.repository.LAODataSource.Local;
import com.github.dedis.popstellar.repository.local.dao.LAODao;
import com.github.dedis.popstellar.repository.local.entities.*;

import java.util.List;

import javax.inject.Inject;

public class LAOLocalDataSource implements Local {

  private final LAODao laoDao;

  @Inject
  public LAOLocalDataSource(LAODatabase database) {
    laoDao = database.laoDao();
  }

  @Override
  public List<LAOEntity> getAll() {
    return laoDao.getAll();
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
      LAOEntity lao, List<PersonEntity> witnesses, List<ModificationSignatureEntity> signatures) {
    laoDao.updateLAO(lao, witnesses, signatures);
  }

  @Override
  public void addRollCall(LAOEntity lao, RollCallEntity rollCall) {
    laoDao.addRollCall(lao, rollCall);
  }

  @Override
  public void updateRollCall(RollCallEntity rollCall) {
    // TODO to be implemented
    throw new UnsupportedOperationException("To be implemented");
  }

  @Override
  public void addMeeting(LAOEntity lao, MeetingEntity meeting) {
    laoDao.addMeeting(lao, meeting);
  }

  @Override
  public void updateMeeting(MeetingEntity meeting, List<ModificationSignatureEntity> signatures) {
    laoDao.updateMeeting(meeting, signatures);
  }
}
