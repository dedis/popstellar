package com.github.dedis.student20_pop;

public class Event<T> {

  private T mContent;

  private boolean hasBeenHandled = false;

  public Event(T content) {
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
