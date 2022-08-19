package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.RollCallCreateFragmentBinding;
import com.github.dedis.popstellar.ui.detail.*;
import com.github.dedis.popstellar.ui.detail.event.AbstractEventCreationFragment;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

/** Fragment that shows up when user wants to create a Roll-Call Event */
@AndroidEntryPoint
public final class RollCallCreationFragment extends AbstractEventCreationFragment {

  public static final String TAG = RollCallCreationFragment.class.getSimpleName();

  private RollCallCreateFragmentBinding mFragBinding;
  private LaoDetailViewModel mLaoDetailViewModel;
  private EditText rollCallTitleEditText;
  private Button confirmButton;
  private Button openButton;

  private final TextWatcher confirmTextWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          // Nothing needed here
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          String meetingTitle = rollCallTitleEditText.getText().toString().trim();
          boolean areFieldsFilled =
              !meetingTitle.isEmpty() && !getStartDate().isEmpty() && !getStartTime().isEmpty();
          confirmButton.setEnabled(areFieldsFilled);
          openButton.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable s) {
          // Nothing needed here
        }
      };

  public static final CompositeDisposable disposables = new CompositeDisposable();

  public static RollCallCreationFragment newInstance() {
    return new RollCallCreationFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    mFragBinding = RollCallCreateFragmentBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    setDateAndTimeView(mFragBinding.getRoot());
    addStartDateAndTimeListener(confirmTextWatcher);

    rollCallTitleEditText = mFragBinding.rollCallTitleText;
    rollCallTitleEditText.addTextChangedListener(confirmTextWatcher);

    openButton = mFragBinding.rollCallOpen;
    confirmButton = mFragBinding.rollCallConfirm;
    confirmButton.setEnabled(false);
    openButton.setEnabled(false);

    mFragBinding.setLifecycleOwner(getActivity());

    return mFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupConfirmButton();
    setupOpenButton();
    setupCancelButton();
  }

  private void setupConfirmButton() {
    confirmButton.setOnClickListener(v -> createRollCall(false));
  }

  private void setupOpenButton() {
    openButton.setOnClickListener(v -> createRollCall(true));
  }

  private void setupCancelButton() {
    mFragBinding.rollCallCancel.setOnClickListener(
        v -> mLaoDetailViewModel.setCurrentTab(LaoTab.EVENTS));
  }

  private void createRollCall(boolean open) {
    if (!computeTimesInSeconds()) {
      return;
    }

    String title = mFragBinding.rollCallTitleText.getText().toString();
    String description = mFragBinding.rollCallEventDescriptionText.getText().toString();
    Single<String> createRollCall =
        mLaoDetailViewModel.createNewRollCall(
            title, description, creationTimeInSeconds, startTimeInSeconds, endTimeInSeconds);

    if (open) {
      disposables.add(
          createRollCall
              .flatMapCompletable(mLaoDetailViewModel::openRollCall)
              .subscribe(
                  // Open the scanning fragment when everything is done
                  () -> mLaoDetailViewModel.openRollCallScanning(requireActivity()),
                  error ->
                      ErrorUtils.logAndShow(
                          requireContext(), TAG, error, R.string.error_create_rollcall)));
    } else {
      disposables.add(
          createRollCall.subscribe(
              id -> mLaoDetailViewModel.setCurrentTab(LaoTab.EVENTS),
              error ->
                  ErrorUtils.logAndShow(
                      requireContext(), TAG, error, R.string.error_create_rollcall)));
    }
  }
}
