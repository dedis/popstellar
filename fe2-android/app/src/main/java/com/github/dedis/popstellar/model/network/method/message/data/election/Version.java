package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Version of an election:
 * - OPEN_BALLOT: normal process
 * - SECRET_BALLOT: vote is encrypted with EL_GAMAL encryption scheme
 */
public enum Version {

  OPEN_BALLOT("open-ballot"),
  SECRET_BALLOT("secret-ballot");

  private final String version;
  private static final List<Version> ALL = Collections.unmodifiableList(Arrays.asList(values()));

  Version (String version) {
    this.version = version;
  }

  public String getStringVersion() {
    return version;
  }

  public static List<Version> getAllVersion() {
    return ALL;
  }

}

