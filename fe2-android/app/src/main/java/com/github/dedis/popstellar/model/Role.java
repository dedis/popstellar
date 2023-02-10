package com.github.dedis.popstellar.model;

import androidx.annotation.StringRes;

import com.github.dedis.popstellar.R;

public enum Role {
  ORGANIZER(R.string.organizer),
  WITNESS(R.string.witness),
  ATTENDEE(R.string.attendee),
  MEMBER(R.string.member);

  @StringRes private final int roleString;

  Role(@StringRes int role) {
    this.roleString = role;
  }

  @StringRes
  public int getStringId() {
    return roleString;
  }
}
