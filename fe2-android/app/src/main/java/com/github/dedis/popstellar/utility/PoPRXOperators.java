package com.github.dedis.popstellar.utility;

import java.util.function.Consumer;

import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class PoPRXOperators {

  /**
   * This function creates an observable operator that suppresses errors to let the underlying
   * subscription running when an error occurs.
   *
   * <p>Its goal is to allow infinite observables to keep pushing new values even when an error
   * occurs
   *
   * @param onError consumes received errors
   * @param <T> type of values
   * @return the new observer
   */
  public static <T> ObservableOperator<T, T> suppressErrors(Consumer<Throwable> onError) {
    return observer ->
        new Observer<T>() {
          @Override
          public void onSubscribe(Disposable d) {
            observer.onSubscribe(d);
          }

          @Override
          public void onNext(T value) {
            observer.onNext(value);
          }

          @Override
          public void onError(Throwable e) {
            onError.accept(e);
          }

          @Override
          public void onComplete() {
            observer.onComplete();
          }
        };
  }
}
