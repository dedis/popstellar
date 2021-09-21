package com.github.dedis.popstellar.utility.scheduler;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class ProdSchedulerProvider implements SchedulerProvider {

  @Override
  public Scheduler io() {
    return Schedulers.io();
  }

  @Override
  public Scheduler computation() {
    return Schedulers.computation();
  }

  @Override
  public Scheduler newThread() {
    return Schedulers.newThread();
  }
}
