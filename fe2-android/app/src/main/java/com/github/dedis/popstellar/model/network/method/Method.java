package com.github.dedis.popstellar.model.network.method;

import java.util.*;

/** Enumerate the different low level messages' method */
public enum Method {
  SUBSCRIBE("subscribe", Subscribe.class, true),
  UNSUBSCRIBE("unsubscribe", Unsubscribe.class, true),
  PUBLISH("publish", Publish.class, true),
  MESSAGE("broadcast", Broadcast.class, false),
  CATCHUP("catchup", Catchup.class, true);

  private static final List<Method> ALL = Collections.unmodifiableList(Arrays.asList(values()));

  private final String method;
  private final Class<? extends Message> dataClass;
  private final boolean expectResult;

  /**
   * Constructor for the Method
   *
   * @param method the name of the method
   * @param dataClass the data class (publish/broadcast/catchup/subscribe/unsubscribe)
   * @param expectResult the expect result as a boolean
   */
  Method(String method, Class<? extends Message> dataClass, boolean expectResult) {
    this.method = method;
    this.dataClass = dataClass;
    this.expectResult = expectResult;
  }

  /** Returns the name of the Method. */
  public String getMethod() {
    return method;
  }

  /** Returns the data class of the Method. */
  public Class<? extends Message> getDataClass() {
    return dataClass;
  }

  /** Returns the expected result of the Method. */
  public boolean expectResult() {
    return expectResult;
  }

  /**
   * Find a given Method
   *
   * @param searched the searched method
   * @return the corresponding enum method
   */
  public static Method find(String searched) {
    for (Method method : ALL) {
      if (method.method.equals(searched)) {
        return method;
      }
    }
    return null;
  }
}
