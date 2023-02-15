package com.github.dedis.popstellar.utility.error;

public abstract class UnknownEventException extends GenericException {

  protected UnknownEventException(String eventType, String id) {
    super(eventType + " with id " + id + " is unknown.");
  }
}
