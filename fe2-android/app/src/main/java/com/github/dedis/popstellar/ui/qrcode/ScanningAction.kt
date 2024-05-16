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
import com.github.dedis.popstellar.utility.UIUtils.InputFieldConfig
import com.github.dedis.popstellar.ui.lao.federation.LinkedOrganizationsFragment
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * Enum class modeling the the action we want to do when using the QR code fragment. It provides
 * strings to display, UI, Input elements and functions, i.e. to obtain view models and on back
 * press behaviour
 *
 * @param instruction the instruction to display to the user
 * @param scanTitle the title to display when scanning
 * @param pageTitle the title to display on the page
 * @param manualAddTitle the title to display when manually adding
 * @param inputFields the input fields to display
 * @param jsonFormatter the function to format the input fields into a JSON string
 * @param scannerViewModelProvider the function to obtain the scanner view model
 * @param popViewModelProvider the function to obtain the PoP view model
 * @param onBackPressed the function to call when the back button is pressed
 * @param displayCounter whether to display the counter (i.e. number of scanned items)
 */
enum class ScanningAction(
    @StringRes val instruction: Int,
    @StringRes val scanTitle: Int,
    @StringRes val pageTitle: Int,
    @StringRes val manualAddTitle: Int,
    private val inputFields: Array<InputFieldConfig>,
    private val jsonFormatter: (Map<Int, String?>) -> String,
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
      R.string.manual_add_witness_title,
      arrayOf(InputFieldConfig(R.string.manual_add_witness_hint, true)),
      { inputs -> "{\"main_public_key\":\"${inputs[R.string.manual_add_witness_hint]}\"}" },
      { activity, _ -> HomeActivity.obtainWitnessingViewModel(activity) },
      { activity -> HomeActivity.obtainViewModel(activity) },
      { manager, _ ->
        HomeActivity.setCurrentFragment(manager, R.id.fragment_lao_create) { LaoCreateFragment() }
      },
      true),
  ADD_WITNESS(
      R.string.qrcode_scanning_add_witness,
      R.string.scanned_witness,
      R.string.add_witness_title,
      R.string.manual_add_witness_title,
      arrayOf(InputFieldConfig(R.string.manual_add_witness_hint, true)),
      { inputs -> "{\"main_public_key\":\"${inputs[R.string.manual_add_witness_hint]}\"}" },
      { activity, laoId -> LaoActivity.obtainWitnessingViewModel(activity, laoId) },
      { activity -> LaoActivity.obtainViewModel(activity) },
      { manager, _ ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_witnessing) {
          com.github.dedis.popstellar.ui.lao.witness.WitnessingFragment()
        }
      },
      true),
  ADD_ROLL_CALL_ATTENDEE(
      R.string.qrcode_scanning_add_attendee,
      R.string.scanned_tokens,
      R.string.add_attendee_title,
      R.string.manual_add_rc_title,
      arrayOf(InputFieldConfig(R.string.manual_add_rc_add_hint, true)),
      { inputs -> "{\"pop_token\":\"${inputs[R.string.manual_add_rc_add_hint]}\"}" },
      { activity, laoId -> LaoActivity.obtainRollCallViewModel(activity, laoId) },
      { activity -> LaoActivity.obtainViewModel(activity) },
      { manager, args ->
        LaoActivity.setCurrentFragment(manager, R.id.fragment_roll_call) {
          RollCallFragment.newInstance(args[0])
        }
      },
      true),
  ADD_LAO_PARTICIPANT(
      R.string.qrcode_scanning_connect_lao,
      R.string.scanned_tokens,
      R.string.join_lao_title,
      R.string.manual_add_lao_join_title,
      arrayOf(
          InputFieldConfig(R.string.manual_add_server_url_hint, true),
          InputFieldConfig(R.string.manual_add_lao_id_hint, true)),
      { inputs ->
        "{\"server\":\"${inputs[R.string.manual_add_server_url_hint]}\", \"lao\":\"${inputs[R.string.manual_add_lao_id_hint]}\"}"
      },
      { activity, _ -> HomeActivity.obtainViewModel(activity) },
      { activity -> HomeActivity.obtainViewModel(activity) },
      { manager, _ ->
        HomeActivity.setCurrentFragment(manager, R.id.fragment_home) { HomeFragment() }
      },
      false),
  ADD_POPCHA(
      R.string.qrcode_scanning_add_popcha,
      R.string.scanned_tokens,
      R.string.popcha_add_title,
      R.string.manual_add_popcha_title,
      arrayOf(InputFieldConfig(R.string.manual_add_popcha_hint, true)),
      { inputs -> inputs[R.string.manual_add_popcha_hint] ?: "" },
      { activity, laoId -> LaoActivity.obtainPoPCHAViewModel(activity, laoId) },
      { activity -> LaoActivity.obtainViewModel(activity) },
      { manager, _ ->
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

  /** Returns the input fields for the scanning action */
  fun getInputFields(): Array<InputFieldConfig> = inputFields

  /**
   * Formats the input fields into a JSON string
   *
   * @param inputs the input fields
   * @return the formatted JSON string, or an empty string if any input is null or blank
   */
  fun formatJson(inputs: Map<Int, String?>): String {
    return if (inputs.values.any { it.isNullOrBlank() }) "" else jsonFormatter(inputs)
  }
}
