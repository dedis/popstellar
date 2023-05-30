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
  REACTION("reaction"),
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

  /**
   * Function that tells whether the given object type has to be persisted.
   *
   * @return true if it is going to be saved on disk, false if only in memory
   */
  public boolean hasToBePersisted() {
    switch (object) {
      case "lao":
      case "election":
      case "roll_call":
      case "chirp":
      case "reaction":
      case "meeting":
      case "coin":
      case "message":
        return true;
        // TODO: add persistence for consensus when it'll have its repo
      case "consensus":
      default:
        return false;
    }
  }
}
