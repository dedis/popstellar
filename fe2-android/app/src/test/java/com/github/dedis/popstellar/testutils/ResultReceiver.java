package com.github.dedis.popstellar.testutils;

import android.os.Bundle;

import com.github.dedis.popstellar.testutils.fragment.FragmentScenario;

import java.util.concurrent.*;

/**
 * Helper object used when creating a fake result listener of a fragment.
 *
 * <p>It is thread-safe and makes sure the object was received.
 */
public class ResultReceiver<T> {

  // locked until a value is set
  private final Semaphore lock = new Semaphore(0);
  private T value;

  public void set(T val) {
    value = val;
    lock.release();
  }

  public T get(long timeout) throws InterruptedException, TimeoutException {
    // Make sure that we received the value. Or wait for the given timeout to get it.
    if (!lock.tryAcquire(timeout, TimeUnit.MILLISECONDS)) throw new TimeoutException();

    return value;
  }

  public boolean received() {
    return lock.availablePermits() > 0;
  }

  public static ResultReceiver<Bundle> createFakeListener(
      FragmentScenario<?, ?> scenario, String requestKey) {
    ResultReceiver<Bundle> receiver = new ResultReceiver<>();
    scenario.onFragment(
        f ->
            f.getParentFragmentManager()
                .setFragmentResultListener(
                    requestKey, f.getViewLifecycleOwner(), (r, b) -> receiver.set(b)));
    return receiver;
  }

  public static <T> ResultReceiver<T> createFakeListener(
      FragmentScenario<?, ?> scenario, String requestKey, String bundleKey) {
    ResultReceiver<T> receiver = new ResultReceiver<>();
    //noinspection unchecked
    scenario.onFragment(
        f ->
            f.getParentFragmentManager()
                .setFragmentResultListener(
                    requestKey,
                    f,
                    // Listener that sets the value of the receiver to the value stored at bundleKey
                    (r, b) -> receiver.set((T) b.get(bundleKey))));
    return receiver;
  }
}
