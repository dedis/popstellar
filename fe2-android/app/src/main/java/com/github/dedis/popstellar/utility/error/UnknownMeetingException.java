package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

public class UnknownMeetingException extends UnknownEventException {

  public UnknownMeetingException(String id) {
    super("Meeting", id);
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_meeting_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
