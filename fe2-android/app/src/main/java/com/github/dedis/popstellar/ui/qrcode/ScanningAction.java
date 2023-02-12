package com.github.dedis.popstellar.ui.qrcode;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.github.dedis.popstellar.R;

/** Enum class modeling the the action we want to do when using the QR code fragment */
public enum ScanningAction {
  ADD_WITNESS {
    @NonNull
    @Override
    public String toString() {
      return "Add witness";
    }

    @Override
    @StringRes
    public int instructions() {
      return R.string.qrcode_scanning_add_attendee;
    }

    @Override
    int scanningTitle() {
      return R.string.scanned_witness;
    }

    @Override
    @StringRes
    public int hint() {
      return R.string.manual_witness_hint;
    }
  },

  ADD_ROLL_CALL_ATTENDEE {
    @NonNull
    @Override
    public String toString() {
      return "Add attendee";
    }

    @Override
    @StringRes
    public int instructions() {
      return R.string.qrcode_scanning_add_witness;
    }

    @Override
    @StringRes
    int scanningTitle() {
      return R.string.scanned_tokens;
    }

    @Override
    @StringRes
    public int hint() {
      return R.string.rc_manual_hint;
    }
  },

  ADD_LAO_PARTICIPANT {
    @NonNull
    @Override
    public String toString() {
      return "Add LAO participant";
    }

    @Override
    @StringRes
    public int instructions() {
      return R.string.qrcode_scanning_connect_lao;
    }

    @Override
    int scanningTitle() {
      // This does not matter as the view has visibility gone for LAO joining
      // Nevertheless, a valid string res must be selected
      return R.string.scanned_tokens;
    }

    @Override
    @StringRes
    public int hint() {
      return R.string.join_manual_hint;
    }
  };

  abstract int hint();

  abstract int instructions();

  abstract int scanningTitle();
}
