package com.github.dedis.student20_pop.utility.scheduler;

import io.reactivex.Scheduler;

public interface SchedulerProvider {

  Scheduler io();

  Scheduler computation();

  Scheduler newThread();
}
