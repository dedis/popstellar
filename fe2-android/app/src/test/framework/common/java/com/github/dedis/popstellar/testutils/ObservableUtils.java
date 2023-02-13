package com.github.dedis.popstellar.testutils;

import io.reactivex.observers.TestObserver;

public class ObservableUtils {

  /**
   * Assert that the current value of the observer is the expected one
   *
   * @param observer to assert on
   * @param expected value
   * @param <T> type of the value
   */
  public static <T> void assertCurrentValueIs(TestObserver<T> observer, T expected) {
    observer.assertValueAt(observer.valueCount() - 1, expected);
  }
}
