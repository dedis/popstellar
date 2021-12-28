package com.github.dedis.popstellar.model.network.method.message.data;

/** An abstract high level message */
public abstract class Data {

  /** Returns the object the message is referring to. */
  public abstract String getObject();

  /** Returns the action the message is handling. */
  public abstract String getAction();
}
