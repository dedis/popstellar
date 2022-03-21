package com.github.dedis.popstellar.utility.scheduler;

import io.reactivex.Scheduler;

public interface SchedulerProvider {

  Scheduler io();

  Scheduler computation();

  Scheduler newThread();

  Scheduler mainThread();
}
