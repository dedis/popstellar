package com.github.dedis.popstellar.ui.lao.event.meeting;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.MeetingCreateFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.AbstractEventCreationFragment;
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallCreationFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;

/** Fragment that shows up when user wants to create a Meeting Event */
@AndroidEntryPoint
public class MeetingCreationFragment extends AbstractEventCreationFragment {
  public static final String TAG = RollCallCreationFragment.class.getSimpleName();

  private MeetingCreateFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private MeetingViewModel meetingViewModel;
  private EditText meetingTitleEditText;
  private Button confirmButton;

  private final TextWatcher confirmTextWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          // Nothing needed here
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          String meetingTitle = meetingTitleEditText.getText().toString().trim();
          String location =
              Objects.requireNonNull(binding.meetingEventLocationText.getText()).toString().trim();
          boolean areFieldsFilled =
              !meetingTitle.isEmpty()
                  && !getStartDate().isEmpty()
                  && !getStartTime().isEmpty()
                  && !location.isEmpty();

          confirmButton.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable s) {
          // Nothing needed here
        }
      };

  public static MeetingCreationFragment newInstance() {
    return new MeetingCreationFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = MeetingCreateFragmentBinding.inflate(inflater, container, false);

    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    meetingViewModel =
        LaoActivity.obtainMeetingViewModel(requireActivity(), laoViewModel.getLaoId());

    setDateAndTimeView(binding.getRoot());
    addStartDateAndTimeListener(confirmTextWatcher);

    meetingTitleEditText = binding.meetingTitleText;
    meetingTitleEditText.addTextChangedListener(confirmTextWatcher);
    binding.meetingEventLocationText.addTextChangedListener(confirmTextWatcher);

    confirmButton = binding.meetingConfirm;
    confirmButton.setEnabled(false);

    binding.setLifecycleOwner(getActivity());

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupConfirmButton();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.meeting_setup_title);
    laoViewModel.setIsTab(false);
  }

  private void setupConfirmButton() {
    confirmButton.setOnClickListener(v -> createMeeting());
  }

  private void createMeeting() {
    if (!computeTimesInSeconds()) {
      return;
    }

    String title = Objects.requireNonNull(binding.meetingTitleText.getText()).toString();
    String location = Objects.requireNonNull(binding.meetingEventLocationText.getText()).toString();
    Single<String> createMeeting =
        meetingViewModel.createNewMeeting(
            title, location, creationTimeInSeconds, startTimeInSeconds, endTimeInSeconds);

    laoViewModel.addDisposable(
        createMeeting.subscribe(
            id ->
                LaoActivity.setCurrentFragment(
                    getParentFragmentManager(),
                    R.id.fragment_event_list,
                    EventListFragment::newInstance),
            error ->
                ErrorUtils.logAndShow(
                    requireContext(), TAG, error, R.string.error_create_meeting)));
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG, "event list", () -> EventListFragment.openFragment(getParentFragmentManager())));
  }
}
