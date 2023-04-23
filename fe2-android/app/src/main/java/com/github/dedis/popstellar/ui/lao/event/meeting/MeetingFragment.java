package com.github.dedis.popstellar.ui.lao.event.meeting;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.MeetingFragmentBinding;
import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.repository.MeetingRepository;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownMeetingException;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static com.github.dedis.popstellar.utility.Constants.MEETING_ID;
import static com.github.dedis.popstellar.utility.Constants.ROLL_CALL_ID;

@AndroidEntryPoint
public class MeetingFragment extends Fragment {
  private static final String TAG = MeetingFragment.class.getSimpleName();
  @Inject Gson gson;
  @Inject MeetingRepository meetingRepo;

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  private MeetingFragmentBinding binding;

  private LaoViewModel laoViewModel;
  private Meeting meeting;

  private MeetingViewModel meetingViewModel;

  public MeetingFragment() {
    // Empty constructor
  }

  public static MeetingFragment newInstance(String persistentId) {
    MeetingFragment fragment = new MeetingFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(Constants.MEETING_ID, persistentId);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    binding = MeetingFragmentBinding.inflate(inflater, container, false);
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    meetingViewModel =
        LaoActivity.obtainMeetingViewModel(requireActivity(), laoViewModel.getLaoId());

    try {
      meeting =
          meetingRepo.getMeetingWithPersistentId(
              laoViewModel.getLaoId(), requireArguments().getString(MEETING_ID));
    } catch (UnknownMeetingException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_meeting_exception);
      return null;
    }

    setUpStateDependantContent();

    laoViewModel.addDisposable(
        meetingViewModel
            .getMeetingObservable(meeting.getPersistentId())
            .subscribe(
                m -> {
                  Log.d(TAG, "Received meeting update: " + m);
                  meeting = m;
                  setUpStateDependantContent();
                },
                error ->
                    ErrorUtils.logAndShow(
                        requireContext(), TAG, error, R.string.unknown_meeting_exception)));

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.meeting_title);
    laoViewModel.setIsTab(false);
    try {
      meeting =
          meetingRepo.getMeetingWithPersistentId(
              laoViewModel.getLaoId(), requireArguments().getString(ROLL_CALL_ID));
    } catch (UnknownMeetingException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_meeting_exception);
    }
  }

  private void setUpStateDependantContent() {
    setupTime();
    binding.meetingTitle.setText(meeting.getName());
    binding.meetingLocationText.setText(meeting.getLocation());
  }

  private void setupTime() {
    if (meeting == null) {
      return;
    }
    Date startTime = new Date(meeting.getStartTimestampInMillis());
    Date endTime = new Date(meeting.getEndTimestampInMillis());

    binding.meetingStartTime.setText(dateFormat.format(startTime));
    binding.meetingEndTime.setText(dateFormat.format(endTime));
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(requireActivity(), getViewLifecycleOwner(), TAG);
  }
}
