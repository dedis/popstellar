package com.github.dedis.popstellar.ui.qrcode;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallFragment;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeFragment;

/** Enum class modeling the the action we want to do when using the QR code fragment */
public enum ScanningAction {
  ADD_WITNESS {

    @Override
    @StringRes
    public int instructions() {
      return R.string.qrcode_scanning_add_witness;
    }

    @Override
    int scanningTitle() {
      return R.string.scanned_witness;
    }

    @Override
    int pageTitle() {
      return R.string.add_witness_title;
    }

    @Override
    QRCodeScanningViewModel obtainViewModel(FragmentActivity activity) {
      return LaoDetailActivity.obtainViewModel(activity);
    }

    @Override
    OnBackPressedCallback onBackPressedCallback(FragmentManager manager, String _unused) {
      return new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
          LaoDetailActivity.setCurrentFragment(
              manager, R.id.fragment_witnessing, WitnessingFragment::new);
        }
      };
    }

    @Override
    @StringRes
    public int hint() {
      return R.string.manual_witness_hint;
    }
  },

  ADD_ROLL_CALL_ATTENDEE {

    @Override
    @StringRes
    public int instructions() {
      return R.string.qrcode_scanning_add_attendee;
    }

    @Override
    @StringRes
    int scanningTitle() {
      return R.string.scanned_tokens;
    }

    @Override
    int pageTitle() {
      return R.string.add_attendee_title;
    }

    @Override
    QRCodeScanningViewModel obtainViewModel(FragmentActivity activity) {
      return LaoDetailActivity.obtainViewModel(activity);
    }

    @Override
    OnBackPressedCallback onBackPressedCallback(FragmentManager manager, String rcPersistentId) {
      return new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
          LaoDetailActivity.setCurrentFragment(
              manager, R.id.fragment_roll_call, () -> RollCallFragment.newInstance(rcPersistentId));
        }
      };
    }

    @Override
    @StringRes
    public int hint() {
      return R.string.rc_manual_hint;
    }
  },

  ADD_LAO_PARTICIPANT {
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
    int pageTitle() {
      return R.string.join_lao_title;
    }

    @Override
    QRCodeScanningViewModel obtainViewModel(FragmentActivity activity) {
      return HomeActivity.obtainViewModel(activity);
    }

    @Override
    OnBackPressedCallback onBackPressedCallback(FragmentManager manager, String _unused) {
      return new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
          HomeActivity.setCurrentFragment(manager, R.id.fragment_home, HomeFragment::new);
        }
      };
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

  abstract int pageTitle();

  abstract QRCodeScanningViewModel obtainViewModel(FragmentActivity activity);

  abstract OnBackPressedCallback onBackPressedCallback(
      FragmentManager manager, String rcPersistentId);
}
