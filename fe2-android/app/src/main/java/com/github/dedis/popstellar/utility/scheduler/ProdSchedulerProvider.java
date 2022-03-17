package com.github.dedis.popstellar.utility.scheduler;

import javax.inject.Inject;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ProdSchedulerProvider implements SchedulerProvider {

  @Inject
  public ProdSchedulerProvider() {
    // This constructor is needed because Hilt needs a constructor annotated with @Inject
    // to find out how the type should be provided
  }

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

  @Override
  public Scheduler mainThread() {
    return AndroidSchedulers.mainThread();
  }
}
