package com.github.dedis.popstellar.model.network.method.message.data.election;

/**
 * Version of an election:
 * - OPEN_BALLOT: normal process
 * - SECRET_BALLOT: vote is encrypted with EL_GAMAL encryption scheme
 */
public enum Version {

  OPEN_BALLOT("open-ballot"),
  SECRET_BALLOT("secret-ballot");

  private final String version;

  Version (String version) {
    this.version = version;
  }

  public String getStringVersion() {
    return version;
  }
}

