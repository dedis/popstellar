package com.github.dedis.student20_pop.model.network;

import com.github.dedis.student20_pop.utility.protocol.MessageHandler;

/** A generic low-level message */
public abstract class GenericMessage {

  /**
   * Accept the given handler following a visitor pattern TODO: Possibly remove this
   *
   * @param handler that will handle the message
   */
  public abstract void accept(MessageHandler handler);

  // public abstract boolean isValid();
}
