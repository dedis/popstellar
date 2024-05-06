package com.github.dedis.popstellar.ui.qrcode

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.ui.PopViewModel
import com.github.dedis.popstellar.ui.home.HomeActivity
import com.github.dedis.popstellar.ui.home.HomeFragment
import com.github.dedis.popstellar.ui.home.LaoCreateFragment
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallFragment
import com.github.dedis.popstellar.ui.lao.federation.LinkedOrganizationsFragment
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * Enum class modeling the the action we want to do when using the QR code fragment. It provides
 * strings to display and functions, i.e. to obtain view models and on back press behaviour
 */
enum class ScanningAction(
    @StringRes val instruction: Int,
    @StringRes val scanTitle: Int,
    @StringRes val pageTitle: Int,
    @StringRes val hint: Int,
    @StringRes val manualAddTitle: Int,
    private val scannerViewModelProvider:
        BiFunction<FragmentActivity, String?, QRCodeScanningViewModel>,
    private val popViewModelProvider: Function<FragmentActivity, PopViewModel>,
    private val onBackPressed: BiConsumer<FragmentManager, Array<String>>,
    val displayCounter: Boolean
) {
  ADD_WITNESS_AT_START(
      R.string.qrcode_scanning_add_witness,
      R.string.scanned_witness,
      R.string.add_witness_title,
      R.string.manual_witness_hint,
      R.string.add_witness_title,
      { activity: FragmentActivity, _: String? ->
        HomeActivity.obtainWitnessingViewModel(activity)
      },
      { activity: FragmentActivity -> HomeActivity.obtainViewModel(activity) },
      { manager: FragmentManager, _: Array<String> ->
        HomeActivity.setCurrentFragment(manager, R.id.fragment_lao_create) { LaoCreateFragment() }
      },
      true),
  ADD_WITNESS(
      R.string.qrcode_scanning_add_witness,
      R.string.scanned_witness,
      R.string.add_witness_title,
      R.string.manual_witness_hint,
      R.string.add_witness_title,
      { activity: FragmentActivity, laoId: String? ->
        LaoActivity.obtainWitnessingViewModel(activity, laoId)
      },
      { activity: FragmentActivity -> LaoActivity.obtainViewModel(activity) },
      { manager: FragmentManager, _: Array<String> ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_witnessing) {
          com.github.dedis.popstellar.ui.lao.witness.WitnessingFragment()
        }
      },
      true),
  ADD_ROLL_CALL_ATTENDEE(
      R.string.qrcode_scanning_add_attendee,
      R.string.scanned_tokens,
      R.string.add_attendee_title,
      R.string.rc_manual_hint,
      R.string.add_attendee_title,
      { activity: FragmentActivity, laoId: String? ->
        LaoActivity.obtainRollCallViewModel(activity, laoId)
      },
      { activity: FragmentActivity -> LaoActivity.obtainViewModel(activity) },
      { manager: FragmentManager, stringArray: Array<String> ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_roll_call) {
          RollCallFragment.newInstance(stringArray[0])
        }
      },
      // We only need the first arg (rc id)
      true),
  ADD_LAO_PARTICIPANT(
      R.string.qrcode_scanning_connect_lao,
      R.string.scanned_tokens,
      R.string.join_lao_title,
      R.string.join_manual_hint,
      R.string.add_lao_participant_title,
      { activity: FragmentActivity, _: String? -> HomeActivity.obtainViewModel(activity) },
      { activity: FragmentActivity -> HomeActivity.obtainViewModel(activity) },
      { manager: FragmentManager, _: Array<String> ->
        HomeActivity.setCurrentFragment(manager, R.id.fragment_home) { HomeFragment() }
      },
      false),
  ADD_POPCHA(
      R.string.qrcode_scanning_add_popcha,
      R.string.scanned_tokens,
      R.string.popcha_add,
      R.string.manual_popcha_hint,
      R.string.popcha_scan_title,
      { activity: FragmentActivity, laoId: String? ->
        LaoActivity.obtainPoPCHAViewModel(activity, laoId)
      },
      { activity: FragmentActivity -> LaoActivity.obtainViewModel(activity) },
      { manager: FragmentManager, _: Array<String> ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_popcha_home) {
          com.github.dedis.popstellar.ui.lao.popcha.PoPCHAHomeFragment()
        }
      },
      false),
  FEDERATION_INVITE(
      R.string.qrcode_scanning_federation,
      R.string.scanned_organizer,
      R.string.invite_other_organization,
      R.string.other_organizer_info,
      R.string.add_other_organizer_info,
      { activity: FragmentActivity, laoId: String? ->
        LaoActivity.obtainLinkedOrganizationsViewModel(activity, laoId)
      },
      { activity: FragmentActivity -> LaoActivity.obtainViewModel(activity) },
      { manager: FragmentManager, _: Array<String> ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_linked_organizations_home) {
          LinkedOrganizationsFragment.newInstance()
        }
      },
      false),
  FEDERATION_JOIN(
      R.string.qrcode_scanning_federation,
      R.string.scanned_organizer,
      R.string.join_other_organization_invitation,
      R.string.other_organizer_info,
      R.string.add_other_organizer_info,
      { activity: FragmentActivity, laoId: String? ->
        LaoActivity.obtainLinkedOrganizationsViewModel(activity, laoId)
      },
      { activity: FragmentActivity -> LaoActivity.obtainViewModel(activity) },
      { manager: FragmentManager, _: Array<String> ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_linked_organizations_home) {
          LinkedOrganizationsFragment.newInstance()
        }
      },
      false);

  /**
   * Provides the view model implementing the QRCodeScanningViewModel interface based on scanning
   * action
   *
   * @param activity the activity of the caller
   * @return QRCodeScanningViewModel view model
   */
  fun obtainScannerViewModel(activity: FragmentActivity, laoId: String?): QRCodeScanningViewModel {
    return scannerViewModelProvider.apply(activity, laoId)
  }

  fun obtainPopViewModel(activity: FragmentActivity): PopViewModel {
    return popViewModelProvider.apply(activity)
  }

  /**
   * Call back that describes the action to take (i.e. which Fragment to open) when back arrow is
   * pressed
   *
   * @param manager needed to open a fragment
   * @param data data necessary (i.e. rc id when opening the RC fragment) to open target fragment
   * @return the callback describing what to do on back button pressed
   */
  fun onBackPressedCallback(manager: FragmentManager, data: Array<String>): OnBackPressedCallback {
    return object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        onBackPressed.accept(manager, data)
      }
    }
  }

  internal fun interface BiFunction<T, U, V> {
    fun apply(t: T, u: U): V
  }
}
