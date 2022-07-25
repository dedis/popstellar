package com.github.dedis.popstellar.testutils;

import android.os.Bundle;

import java.io.Serializable;

/**
 * Helper class to build {@link Bundle}
 *
 * <p>The current Bundle implementation needs multiple lines to be created. This is an important
 * limitation when writing tests because bundles are often needed by fields.
 *
 * <p>Ex: When creating an {@link android.app.Activity} that takes extra arguments, a bundle is
 * needed. But Bundles needs multiple lines to be created. Therefore, everything needs to be done in
 * a static block which is not the cleanest.
 *
 * <p>This class purpose is to bypass this limitation by providing a builder to {@link Bundle}
 *
 * <p>Creation : 04/12/2021
 */
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

  /**
   * @return the built bundle
   */
  public Bundle build() {
    return bundle;
  }
}
