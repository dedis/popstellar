package com.github.dedis.popstellar;

public class SingleEvent<T> {

  private final T mContent;

  private boolean hasBeenHandled = false;

  public SingleEvent(T content) {
    if (content == null) {
      throw new IllegalArgumentException("null values not allowed in an Event");
    }
    mContent = content;
  }

  public T getContentIfNotHandled() {
    if (hasBeenHandled) {
      return null;
    } else {
      hasBeenHandled = true;
      return mContent;
    }
  }

  public boolean hasBeenHandled() {
    return hasBeenHandled;
  }
}
