package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.entities.Meeting;
import com.github.dedis.student20_pop.model.entities.ModificationSignature;
import com.github.dedis.student20_pop.model.entities.Person;
import com.github.dedis.student20_pop.model.entities.RollCall;
import java.util.ArrayList;
import java.util.List;

public class FakeLAOLocalDataSource implements LAODataSource.Local {

  private List<LAO> laoList;
  private List<LAOEntity> laoEntityList;

  private static FakeLAOLocalDataSource INSTANCE;

  private FakeLAOLocalDataSource() {
    laoList = new ArrayList<>();
    laoEntityList = new ArrayList<>();
  }

  public static FakeLAOLocalDataSource getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FakeLAOLocalDataSource();
    }
    return INSTANCE;
  }

  @Override
  public List<LAO> getAll() {
    return laoList;
  }

  @Override
  public LAOEntity getLAO(String channel) {
    return laoEntityList.stream()
        .filter(l -> l.lao.channel.equals(channel))
        .findFirst()
        .orElse(new LAOEntity());
  }

  @Override
  public void addLao(LAO lao) {
    laoList.add(lao);
    LAOEntity laoEntity = new LAOEntity();
    laoEntity.lao = lao;
    laoEntityList.add(laoEntity);
  }

  @Override
  public void updateLAO(LAO lao, List<Person> witnesses, List<ModificationSignature> signatures) {

  }

  @Override
  public void addRollCall(LAO lao, RollCall rollCall) {

  }

  @Override
  public void updateRollCall(RollCall rollCall) {

  }

  @Override
  public void addMeeting(LAO lao, Meeting meeting) {

  }

  @Override
  public void updateMeeting(Meeting meeting, List<ModificationSignature> signatures) {

  }
}
