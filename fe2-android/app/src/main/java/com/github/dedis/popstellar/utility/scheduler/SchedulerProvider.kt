package com.github.dedis.popstellar.utility.scheduler

import io.reactivex.Scheduler

interface SchedulerProvider {
    fun io(): Scheduler
    fun computation(): Scheduler
    fun newThread(): Scheduler
    fun mainThread(): Scheduler?
}