package com.github.dedis.popstellar.repository.remote;

import androidx.annotation.NonNull;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.lifecycle.FlowableLifecycle;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import org.reactivestreams.Subscriber;

/** Java class used as wrapper to access an internal Kotlin declaration */
class FlowableLifecycleInt implements Lifecycle {

  private final FlowableLifecycle internalInstance;

  public FlowableLifecycleInt(Flowable<State> flowable, Scheduler scheduler) {
    this.internalInstance = new FlowableLifecycle(flowable, scheduler);
  }

  @NonNull
  @Override
  public Lifecycle combineWith(@NonNull Lifecycle... lifecycles) {
    return internalInstance.combineWith(lifecycles);
  }

  @Override
  public void subscribe(Subscriber<? super State> s) {
    internalInstance.subscribe(s);
  }
}
