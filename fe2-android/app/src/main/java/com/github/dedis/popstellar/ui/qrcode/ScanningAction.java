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

import java.util.function.BiConsumer;
import java.util.function.Function;

/** Enum class modeling the the action we want to do when using the QR code fragment */
public enum ScanningAction {
  ADD_WITNESS(
      R.string.qrcode_scanning_add_witness,
      R.string.scanned_witness,
      R.string.add_witness_title,
      R.string.manual_witness_hint,
      R.string.add_witness_title,
      LaoDetailActivity::obtainViewModel,
      (manager, unused) ->
          LaoDetailActivity.setCurrentFragment(
              manager, R.id.fragment_witnessing, WitnessingFragment::new)),
  ADD_ROLL_CALL_ATTENDEE(
      R.string.qrcode_scanning_add_attendee,
      R.string.scanned_tokens,
      R.string.add_attendee_title,
      R.string.rc_manual_hint,
      R.string.add_attendee_title,
      LaoDetailActivity::obtainViewModel,
      (manager, rcPersistentId) ->
          LaoDetailActivity.setCurrentFragment(
              manager,
              R.id.fragment_roll_call,
              () -> RollCallFragment.newInstance(rcPersistentId))),
  ADD_LAO_PARTICIPANT(
      R.string.qrcode_scanning_connect_lao,
      R.string.scanned_tokens,
      R.string.join_lao_title,
      R.string.join_manual_hint,
      R.string.add_lao_participant_title,
      HomeActivity::obtainViewModel,
      (manager, unused) ->
          HomeActivity.setCurrentFragment(manager, R.id.fragment_home, HomeFragment::new));

  @StringRes public final int instruction;
  @StringRes public final int scanTitle;
  @StringRes public final int pageTitle;
  @StringRes public final int hint;
  @StringRes public final int manualAddTitle;
  private final Function<FragmentActivity, QRCodeScanningViewModel> viewModelProvider;
  private final BiConsumer<FragmentManager, String> onBackPressed;

  ScanningAction(
      @StringRes int instruction,
      @StringRes int scanTitle,
      @StringRes int pageTitle,
      @StringRes int hint,
      int manualAddTitle,
      Function<FragmentActivity, QRCodeScanningViewModel> viewModelProvider,
      BiConsumer<FragmentManager, String> onBackPressed) {
    this.instruction = instruction;
    this.scanTitle = scanTitle;
    this.pageTitle = pageTitle;
    this.hint = hint;
    this.manualAddTitle = manualAddTitle;
    this.viewModelProvider = viewModelProvider;
    this.onBackPressed = onBackPressed;
  }

  public QRCodeScanningViewModel obtainViewModel(FragmentActivity activity) {
    return viewModelProvider.apply(activity);
  }

  public OnBackPressedCallback onBackPressedCallback(FragmentManager manager, String data) {
    return new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        onBackPressed.accept(manager, data);
      }
    };
  }
}
