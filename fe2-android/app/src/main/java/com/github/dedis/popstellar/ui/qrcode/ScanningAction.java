package com.github.dedis.popstellar.ui.qrcode;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.PopViewModel;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeFragment;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment;
import com.github.dedis.popstellar.ui.lao.witness.WitnessingFragment;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Enum class modeling the the action we want to do when using the QR code fragment. It provides
 * strings to display and functions, i.e. to obtain view models and on back press behaviour
 */
public enum ScanningAction {
  ADD_WITNESS(
      R.string.qrcode_scanning_add_witness,
      R.string.scanned_witness,
      R.string.add_witness_title,
      R.string.manual_witness_hint,
      R.string.add_witness_title,
      LaoActivity::obtainWitnessingViewModel,
      LaoActivity::obtainViewModel,
      (manager, unused) ->
          LaoActivity.setCurrentFragment(
              manager, R.id.fragment_witnessing, WitnessingFragment::new)),
  ADD_ROLL_CALL_ATTENDEE(
      R.string.qrcode_scanning_add_attendee,
      R.string.scanned_tokens,
      R.string.add_attendee_title,
      R.string.rc_manual_hint,
      R.string.add_attendee_title,
      LaoActivity::obtainRollCallViewModel,
      LaoActivity::obtainViewModel,
      (manager, stringArray) ->
          LaoActivity.setCurrentFragment(
              manager,
              R.id.fragment_roll_call,
              () ->
                  RollCallFragment.newInstance(
                      stringArray[0]))), // We only need the first arg (rc id)
  ADD_LAO_PARTICIPANT(
      R.string.qrcode_scanning_connect_lao,
      R.string.scanned_tokens,
      R.string.join_lao_title,
      R.string.join_manual_hint,
      R.string.add_lao_participant_title,
      (activity, unused) -> HomeActivity.obtainViewModel(activity),
      HomeActivity::obtainViewModel,
      (manager, unused) ->
          HomeActivity.setCurrentFragment(manager, R.id.fragment_home, HomeFragment::new));

  @StringRes public final int instruction;
  @StringRes public final int scanTitle;
  @StringRes public final int pageTitle;
  @StringRes public final int hint;
  @StringRes public final int manualAddTitle;
  private final BiFunction<FragmentActivity, String, QRCodeScanningViewModel>
      scannerViewModelProvider;
  private final Function<FragmentActivity, PopViewModel> popViewModelProvider;
  private final BiConsumer<FragmentManager, String[]> onBackPressed;

  ScanningAction(
      @StringRes int instruction,
      @StringRes int scanTitle,
      @StringRes int pageTitle,
      @StringRes int hint,
      int manualAddTitle,
      BiFunction<FragmentActivity, String, QRCodeScanningViewModel> scannerViewModelProvider,
      Function<FragmentActivity, PopViewModel> popViewModelProvider,
      BiConsumer<FragmentManager, String[]> onBackPressed) {
    this.instruction = instruction;
    this.scanTitle = scanTitle;
    this.pageTitle = pageTitle;
    this.hint = hint;
    this.manualAddTitle = manualAddTitle;
    this.scannerViewModelProvider = scannerViewModelProvider;
    this.popViewModelProvider = popViewModelProvider;
    this.onBackPressed = onBackPressed;
  }

  /**
   * Provides the view model implementing the QRCodeScanningViewModel interface based on scanning
   * action
   *
   * @param activity the activity of the caller
   * @return QRCodeScanningViewModel view model
   */
  public QRCodeScanningViewModel obtainScannerViewModel(FragmentActivity activity, String laoId) {
    return scannerViewModelProvider.apply(activity, laoId);
  }

  public PopViewModel obtainPopViewModel(FragmentActivity activity) {
    return popViewModelProvider.apply(activity);
  }

  /**
   * Call back that describes the action to take (i.e. which Fragment to open) when back arrow is
   * pressed
   *
   * @param manager needed to open a fragment
   * @param data data necessary (i.e. rc id when opening the RC fragment) to open target fragment
   * @return the callback describing what to do on back button pressed
   */
  public OnBackPressedCallback onBackPressedCallback(FragmentManager manager, String[] data) {
    return new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        onBackPressed.accept(manager, data);
      }
    };
  }

  @FunctionalInterface
  interface BiFunction<T, U, V> {
    V apply(T t, U u);
  }
}
