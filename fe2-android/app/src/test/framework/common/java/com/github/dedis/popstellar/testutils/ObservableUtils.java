package com.github.dedis.popstellar.testutils;

import io.reactivex.observers.TestObserver;

public class ObservableUtils {

  public static <T> void assertCurrentValueIs(TestObserver<T> ids, T value) {
    ids.assertValueAt(ids.valueCount() - 1, value);
  }
}
