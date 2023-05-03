package com.github.dedis.popstellar.ui.lao.event.meeting;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.MeetingFragmentBinding;
import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.repository.MeetingRepository;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.event.AbstractEventFragment;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownMeetingException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import static com.github.dedis.popstellar.utility.Constants.MEETING_ID;

@AndroidEntryPoint
public class MeetingFragment extends AbstractEventFragment {
  private static final String TAG = MeetingFragment.class.getSimpleName();

  @Inject MeetingRepository meetingRepo;

  private MeetingFragmentBinding binding;

  private Meeting meeting;

  public MeetingFragment() {
    // Required empty public constructor
  }

  public static MeetingFragment newInstance(String id) {
    MeetingFragment fragment = new MeetingFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(MEETING_ID, id);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    binding = MeetingFragmentBinding.inflate(inflater, container, false);
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    MeetingViewModel meetingViewModel =
        LaoActivity.obtainMeetingViewModel(requireActivity(), laoViewModel.getLaoId());

    try {
      meeting =
          meetingRepo.getMeetingWithId(
              laoViewModel.getLaoId(), requireArguments().getString(MEETING_ID));
    } catch (UnknownMeetingException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_meeting_exception);
      return null;
    }

    setUpStateDependantContent();

    laoViewModel.addDisposable(
        meetingViewModel
            .getMeetingObservable(meeting.getId())
            .subscribe(
                m -> {
                  Timber.tag(TAG).d("Received meeting update: %s", m);
                  meeting = m;
                  setUpStateDependantContent();
                },
                error ->
                    ErrorUtils.logAndShow(
                        requireContext(), TAG, error, R.string.unknown_meeting_exception)));

    handleBackNav(TAG);
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    setTab(R.string.meeting_title);
    try {
      meeting =
          meetingRepo.getMeetingWithId(
              laoViewModel.getLaoId(), requireArguments().getString(MEETING_ID));
    } catch (UnknownMeetingException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_meeting_exception);
    }
  }

  private void setUpStateDependantContent() {
    setupTime(meeting, binding.meetingStartTime, binding.meetingEndTime);
    setStatus(meeting.getState(), binding.meetingStatusIcon, binding.meetingStatus);
    binding.meetingTitle.setText(meeting.getName());

    // Set location visible if present
    if (meeting.getLocation().isEmpty()) {
      binding.meetingLocationTitle.setVisibility(View.GONE);
    } else {
      binding.meetingLocationText.setText(meeting.getLocation());
    }
  }
}
