package com.github.dedis.popstellar.utility.scheduler;

import javax.inject.Inject;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class ProdSchedulerProvider implements SchedulerProvider {

  @Inject
  public ProdSchedulerProvider() {}

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
