package com.github.dedis.student20_pop.detail.fragments.event.creation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.github.dedis.student20_pop.databinding.FragmentCreateRollCallEventBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;

import java.time.Instant;

/** Fragment that shows up when user wants to create a Roll-Call Event */
public final class RollCallEventCreationFragment extends AbstractEventCreationFragment {

  public static final String TAG = RollCallEventCreationFragment.class.getSimpleName();

  private EditText rollCallDescriptionEditText;
  private EditText rollCallTitleEditText;
  private Button confirmButton;
  private Button openButton;

  private LaoDetailViewModel mLaoDetailViewModel;

  private final TextWatcher confirmTextWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          String meetingTitle = rollCallTitleEditText.getText().toString().trim();
          boolean areFieldsFilled =
              !meetingTitle.isEmpty() && !getStartDate().isEmpty() && !getStartTime().isEmpty();
          confirmButton.setEnabled(areFieldsFilled);
          openButton.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable s) {}
      };

  public static RollCallEventCreationFragment newInstance() {
    return new RollCallEventCreationFragment();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    FragmentCreateRollCallEventBinding binding =
        FragmentCreateRollCallEventBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    // TODO: refactor this
    setDateAndTimeView(binding.getRoot(), this, getFragmentManager());
    addDateAndTimeListener(confirmTextWatcher);

    rollCallTitleEditText = binding.rollCallTitleText;
    rollCallDescriptionEditText = binding.rollCallEventDescriptionText;

    openButton = binding.rollCallOpen;

    confirmButton = binding.rollCallConfirm;

    // TODO: this has to be replaced by a 'scheduled' button
    //    confirmButton.setOnClickListener(
    //        v -> {
    //          computeTimesInSeconds();
    //
    //          String title = rollCallTitleEditText.getText().toString();
    //          String description = rollCallDescriptionEditText.getText().toString();
    //          long now = Instant.now().getEpochSecond();
    //          long start = startTimeInSeconds > now ? 0 : startTimeInSeconds;
    //          long scheduled = startTimeInSeconds >= now ? startTimeInSeconds : 0;
    //          mLaoDetailViewModel
    //              .createNewRollCall(title, description, start, scheduled, endTimeInSeconds);
    //        });

    openButton.setOnClickListener(
        v -> {
          computeTimesInSeconds();

          String title = rollCallTitleEditText.getText().toString();
          String description = rollCallDescriptionEditText.getText().toString();
          long now = Instant.now().getEpochSecond();
          long start = startTimeInSeconds > now ? 0 : startTimeInSeconds;
          long scheduled = startTimeInSeconds >= now ? startTimeInSeconds : 0;
          mLaoDetailViewModel.createNewRollCall(title, description, start, scheduled);
        });

    Button cancelButton = binding.rollCallCancel;

    cancelButton.setOnClickListener(
        v -> {
          mLaoDetailViewModel.openLaoDetail();
        });

    binding.setLifecycleOwner(getActivity());

    return binding.getRoot();
  }
}
