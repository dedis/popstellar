package com.github.dedis.popstellar.ui.lao.event.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.MeetingFragmentBinding
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainMeetingViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.event.AbstractEventFragment
import com.github.dedis.popstellar.utility.Constants.MEETING_ID
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownMeetingException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class MeetingFragment : AbstractEventFragment() {
  @Inject lateinit var meetingRepo: MeetingRepository

  private lateinit var binding: MeetingFragmentBinding
  private lateinit var meeting: Meeting

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    binding = MeetingFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val meetingViewModel = obtainMeetingViewModel(requireActivity(), laoViewModel.laoId!!)

    meeting =
        try {
          meetingRepo.getMeetingWithId(
              laoViewModel.laoId!!, requireArguments().getString(MEETING_ID)!!)
        } catch (e: UnknownMeetingException) {
          logAndShow(requireContext(), TAG, e, R.string.unknown_meeting_exception)
          return null
        }

    setUpStateDependantContent()

    laoViewModel.addDisposable(
        meetingViewModel
            .getMeetingObservable(meeting.id)
            .subscribe(
                { m: Meeting ->
                  Timber.tag(TAG).d("Received meeting update: %s", m)
                  meeting = m
                  setUpStateDependantContent()
                },
                { error: Throwable ->
                  logAndShow(requireContext(), TAG, error, R.string.unknown_meeting_exception)
                }))

    handleBackNav(TAG)

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    setTab(R.string.meeting_title)
    try {
      meeting =
          meetingRepo.getMeetingWithId(
              laoViewModel.laoId!!, requireArguments().getString(MEETING_ID)!!)
    } catch (e: UnknownMeetingException) {
      logAndShow(requireContext(), TAG, e, R.string.unknown_meeting_exception)
    }
  }

  private fun setUpStateDependantContent() {
    setupTime(meeting, binding.meetingStartTime, binding.meetingEndTime)
    setStatus(meeting.state, binding.meetingStatusIcon, binding.meetingStatus)

    binding.meetingTitle.text = meeting.name

    // Set location visible if present
    if (meeting.location.isEmpty()) {
      binding.meetingLocationTitle.visibility = View.GONE
    } else {
      binding.meetingLocationText.text = meeting.location
    }
  }

  companion object {
    private val TAG = MeetingFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(id: String): MeetingFragment {
      val fragment = MeetingFragment()
      val bundle = Bundle(1)
      bundle.putString(MEETING_ID, id)
      fragment.arguments = bundle

      return fragment
    }
  }
}
