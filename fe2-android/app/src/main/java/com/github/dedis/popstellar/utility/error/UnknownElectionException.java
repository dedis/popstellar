package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

public class UnknownElectionException extends UnknownEventException {

  public UnknownElectionException(String electionId) {
    super("Election", electionId);
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_election_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
