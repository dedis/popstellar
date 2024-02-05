package com.github.dedis.popstellar.utility.scheduler

import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler

class TestSchedulerProvider : SchedulerProvider {
  @JvmField val testScheduler: TestScheduler = TestScheduler()

  override fun io(): Scheduler {
    return testScheduler
  }

  override fun computation(): Scheduler {
    return testScheduler
  }

  override fun newThread(): Scheduler {
    return testScheduler
  }

  override fun mainThread(): Scheduler {
    return testScheduler
  }
}
