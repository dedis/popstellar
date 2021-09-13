package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.event.Event;
import java.util.ArrayList;
import java.util.List;

public class LAODetail {

  private Lao mLao;

  private List<Event> laoEvents;

  public LAODetail(Lao mLao, List<Event> laoEvents) {
    this.mLao = mLao;
    this.laoEvents = laoEvents;
  }

  public LAODetail(Lao mLao) {
    this.mLao = mLao;
    this.laoEvents = new ArrayList<>();
  }

  public Lao getLao() {
    return mLao;
  }

  public List<Event> getLaoEvents() {
    return laoEvents;
  }

  public void setLaoEvents(List<Event> laoEvents) {
    this.laoEvents = laoEvents;
  }

  public void setLao(Lao mLao) {
    this.mLao = mLao;
  }
}
