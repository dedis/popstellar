package com.github.dedis.popstellar.testutils;

import android.os.Bundle;

import java.io.Serializable;

/** Helper class to build {@link Bundle} */
public class BundleBuilder {

  private final Bundle bundle = new Bundle();

  public BundleBuilder putInt(String key, int value) {
    bundle.putInt(key, value);
    return this;
  }

  public BundleBuilder putString(String key, String value) {
    bundle.putString(key, value);
    return this;
  }

  public BundleBuilder putSerializable(String key, Serializable value) {
    bundle.putSerializable(key, value);
    return this;
  }

  /** @return the built bundle */
  public Bundle build() {
    return bundle;
  }
}
