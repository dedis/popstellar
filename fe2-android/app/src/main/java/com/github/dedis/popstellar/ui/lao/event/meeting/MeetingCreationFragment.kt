package com.github.dedis.popstellar.ui.lao.event.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.MeetingCreateFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainMeetingViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.AbstractEventCreationFragment
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment.Companion.openFragment
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import dagger.hilt.android.AndroidEntryPoint

/** Fragment that shows up when user wants to create a Meeting Event */
@AndroidEntryPoint
class MeetingCreationFragment : AbstractEventCreationFragment() {

  private lateinit var binding: MeetingCreateFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var meetingViewModel: MeetingViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = MeetingCreateFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    meetingViewModel = obtainMeetingViewModel(requireActivity(), laoViewModel.laoId!!)

    confirmButton = binding.meetingConfirm
    confirmButton!!.isEnabled = false

    setDateAndTimeView(binding.root)
    val meetingTitleEditText: EditText = binding.meetingTitleText
    val confirmTextWatcher = getConfirmTextWatcher(meetingTitleEditText)

    addStartDateAndTimeListener(confirmTextWatcher)
    meetingTitleEditText.addTextChangedListener(confirmTextWatcher)
    binding.meetingEventLocationText.addTextChangedListener(confirmTextWatcher)

    binding.lifecycleOwner = activity

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.meeting_setup_title)
    laoViewModel.setIsTab(false)
  }

  override fun createEvent() {
    if (!computeTimesInSeconds()) {
      return
    }
    val title = binding.meetingTitleText.text.toString()
    val location = binding.meetingEventLocationText.text.toString()

    val createMeeting =
        meetingViewModel.createNewMeeting(
            title, location, creationTimeInSeconds, startTimeInSeconds, endTimeInSeconds)

    laoViewModel.addDisposable(
        createMeeting.subscribe(
            {
              setCurrentFragment(parentFragmentManager, R.id.fragment_event_list) {
                EventListFragment.newInstance()
              }
            },
            { error: Throwable ->
              logAndShow(requireContext(), TAG, error, R.string.error_create_meeting)
            }))
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "event list") { openFragment(parentFragmentManager) })
  }

  companion object {
    val TAG: String = MeetingCreationFragment::class.java.simpleName

    fun newInstance(): MeetingCreationFragment {
      return MeetingCreationFragment()
    }
  }
}
