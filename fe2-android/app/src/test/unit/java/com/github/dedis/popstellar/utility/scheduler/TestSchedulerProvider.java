package com.github.dedis.popstellar.utility.scheduler;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.TestScheduler;

public class TestSchedulerProvider implements SchedulerProvider {

  private final TestScheduler testScheduler;

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

  @Override
  public Scheduler mainThread() {
    return testScheduler;
  }

  public TestScheduler getTestScheduler() {
    return testScheduler;
  }
}
