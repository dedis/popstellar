package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.data.dao.LAODao;
import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.entities.Meeting;
import com.github.dedis.student20_pop.model.entities.ModificationSignature;
import com.github.dedis.student20_pop.model.entities.Person;
import com.github.dedis.student20_pop.model.entities.RollCall;

import java.util.List;

public class LAOLocalDataSource implements LAODataSource.Local {

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
  public List<LAO> getAll() {
    return null;
  }

  @Override
  public LAOEntity getLAO(String channel) {
    return laoDao.getLAO(channel);
  }

  @Override
  public void addLao(LAO lao) {
    laoDao.addLao(lao);
  }

  @Override
  public void updateLAO(LAO lao, List<Person> witnesses, List<ModificationSignature> signatures) {}

  @Override
  public void addRollCall(LAO lao, RollCall rollCall) {}

  @Override
  public void updateRollCall(RollCall rollCall) {}

  @Override
  public void addMeeting(LAO lao, Meeting meeting) {}

  @Override
  public void updateMeeting(Meeting meeting, List<ModificationSignature> signatures) {}
}
