package com.github.dedis.popstellar.utility.scheduler

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ProdSchedulerProvider @Inject constructor() : SchedulerProvider {
  override fun io(): Scheduler {
    return Schedulers.io()
  }

  override fun computation(): Scheduler {
    return Schedulers.computation()
  }

  override fun newThread(): Scheduler {
    return Schedulers.newThread()
  }

  override fun mainThread(): Scheduler {
    return AndroidSchedulers.mainThread()
  }
}
