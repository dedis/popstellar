package com.github.dedis.popstellar.utility

import io.reactivex.ObservableOperator
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.function.Consumer
import timber.log.Timber

object PoPRXOperators {
  /**
   * This function creates an observable operator that suppresses errors to keep the underlying
   * subscription running when an error occurs.
   *
   * Its goal is to allow infinite observables to keep pushing new values even when an error occurs
   *
   * @param onError consumes received errors
   * @param <T> type of values
   * @return the new observer </T>
   */
  fun <T> suppressErrors(onError: Consumer<Throwable>): ObservableOperator<T, T> {
    return ObservableOperator { observer: Observer<in T> ->
      object : Observer<T> {
        override fun onSubscribe(d: Disposable) {
          observer.onSubscribe(d)
        }

        override fun onNext(t: T & Any) {
          observer.onNext(t)
        }

        override fun onError(e: Throwable) {
          onError.accept(e)
          Timber.e(
              e, "This error was suppressed from a PoPRXOperator to keep the subscription running")
        }

        override fun onComplete() {
          observer.onComplete()
        }
      }
    }
  }
}
