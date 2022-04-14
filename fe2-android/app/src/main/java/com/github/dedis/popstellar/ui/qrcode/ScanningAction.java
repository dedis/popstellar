package com.github.dedis.popstellar.ui.qrcode;

import androidx.annotation.NonNull;

/** Enum class modeling the the action we want to do when using the QR code fragment */
public enum ScanningAction {
  ADD_WITNESS {
    @NonNull
    @Override
    public String toString() {
      return "Add Witnesses ";
    }
  },

  ADD_ROLL_CALL_ATTENDEE {
    @NonNull
    @Override
    public String toString() {
      return "Add Roll Call Attendees";
    }
  },

  ADD_LAO_PARTICIPANT {
    @NonNull
    @Override
    public String toString() {
      return "Add Participants to the LAO";
    }
  },

  POST_TRANSACTION_PARTICIPANT {
    @NonNull
    @Override
    public String toString() {return "Post Transaction to a participant";}
  }
}
