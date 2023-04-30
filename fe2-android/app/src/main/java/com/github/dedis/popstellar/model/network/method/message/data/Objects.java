package com.github.dedis.popstellar.model.network.method.message.data;

import java.util.*;

/** Enumerates all possible messages objects */
public enum Objects {
  LAO("lao"),
  MEETING("meeting"),
  MESSAGE("message"),
  ROLL_CALL("roll_call"),
  ELECTION("election"),
  CONSENSUS("consensus"),
  CHIRP("chirp"),
  COIN("coin");

  private static final List<Objects> ALL = Collections.unmodifiableList(Arrays.asList(values()));
  private final String object;

  /**
   * Constructor for a message Object
   *
   * @param object name of the object
   */
  Objects(String object) {
    this.object = object;
  }

  /** Returns the name of the Object. */
  public String getObject() {
    return object;
  }

  /**
   * Find a given Object
   *
   * @param searched the searched object
   * @return the corresponding enum object
   */
  public static Objects find(String searched) {
    return ALL.stream()
        .filter(object -> object.getObject().equals(searched))
        .findFirst()
        .orElse(null);
  }

  public boolean hasToBePersisted() {
    switch (object) {
      case "lao":
        // TODO: add persistence for election, coin and roll calls
        // case "election":
        // case "coin":
        // case "roll_call":
        // Persist LAO, ELECTION, COIN, ROLL CALL
        return true;
      default:
        // Don't persist MEETING, WITNESSING, SOCIAL MEDIA, CONSENSUS
        return false;
    }
  }
}
