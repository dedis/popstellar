package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.RollCallCreateFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainRollCallViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.AbstractEventCreationFragment
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment.Companion.openFragment
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import dagger.hilt.android.AndroidEntryPoint

/** Fragment that shows up when user wants to create a Roll-Call Event */
@AndroidEntryPoint
class RollCallCreationFragment : AbstractEventCreationFragment() {

  private lateinit var binding: RollCallCreateFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var rollCallViewModel: RollCallViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = RollCallCreateFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    rollCallViewModel = obtainRollCallViewModel(requireActivity(), laoViewModel.laoId)

    confirmButton = binding.rollCallConfirm
    confirmButton!!.isEnabled = false

    setDateAndTimeView(binding.root)
    val rollCallTitleEditText: EditText = binding.rollCallTitleText
    val confirmTextWatcher =
        getConfirmTextWatcher(rollCallTitleEditText, binding.rollCallEventLocationText)

    addStartDateAndTimeListener(confirmTextWatcher)
    rollCallTitleEditText.addTextChangedListener(confirmTextWatcher)

    binding.rollCallEventLocationText.addTextChangedListener(confirmTextWatcher)
    binding.lifecycleOwner = activity

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.roll_call_setup_title)
    laoViewModel.setIsTab(false)
  }

  override fun createEvent() {
    if (!computeTimesInSeconds()) {
      return
    }

    val title = binding.rollCallTitleText.text.toString()
    val description = binding.rollCallEventDescriptionText.text.toString()
    val location = binding.rollCallEventLocationText.text.toString()

    val createRollCall =
        rollCallViewModel.createNewRollCall(
            title,
            description,
            location,
            creationTimeInSeconds,
            startTimeInSeconds,
            endTimeInSeconds)

    laoViewModel.addDisposable(
        createRollCall.subscribe(
            {
              setCurrentFragment(parentFragmentManager, R.id.fragment_event_list) {
                EventListFragment.newInstance()
              }
            },
            { error: Throwable ->
              logAndShow(requireContext(), TAG, error, R.string.error_create_rollcall)
            }))
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "event list") { openFragment(parentFragmentManager) })
  }

  companion object {
    val TAG: String = RollCallCreationFragment::class.java.simpleName

    fun newInstance(): RollCallCreationFragment {
      return RollCallCreationFragment()
    }
  }
}
