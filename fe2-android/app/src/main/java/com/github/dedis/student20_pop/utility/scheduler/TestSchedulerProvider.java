package com.github.dedis.student20_pop.utility.scheduler;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.TestScheduler;

public class TestSchedulerProvider implements SchedulerProvider {

  private Scheduler testScheduler;

  public TestSchedulerProvider() {
    testScheduler = new TestScheduler();
  }

  @Override
  public Scheduler io() {
    return testScheduler;
  }

  @Override
  public Scheduler computation() {
    return testScheduler;
  }

  @Override
  public Scheduler newThread() {
    return testScheduler;
  }
}
