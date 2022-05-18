package com.github.dedis.popstellar.model.network.method.message.data.election;

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

  private final String ballotVersion;
  private static final List<Version> ALL = Collections.unmodifiableList(Arrays.asList(values()));

  Version (String ballotVersion) {
    this.ballotVersion = ballotVersion;
  }

  public String getStringBallotVersion() {
    return ballotVersion;
  }

  public static List<Version> getAllVersion() {
    return ALL;
  }

}

