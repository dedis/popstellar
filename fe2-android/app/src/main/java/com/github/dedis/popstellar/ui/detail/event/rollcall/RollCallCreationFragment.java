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
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

/** Fragment that shows up when user wants to create a Roll-Call Event */
@AndroidEntryPoint
public final class RollCallCreationFragment extends AbstractEventCreationFragment {

  public static final String TAG = RollCallCreationFragment.class.getSimpleName();

  private RollCallCreateFragmentBinding mFragBinding;
  private LaoDetailViewModel viewModel;
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

  public static RollCallCreationFragment newInstance() {
    return new RollCallCreationFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    mFragBinding = RollCallCreateFragmentBinding.inflate(inflater, container, false);

    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

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
  }

  private void setupConfirmButton() {
    confirmButton.setOnClickListener(v -> createRollCall(false));
  }

  private void setupOpenButton() {
    openButton.setOnClickListener(v -> createRollCall(true));
  }

  private void createRollCall(boolean open) {
    if (!computeTimesInSeconds()) {
      return;
    }

    String title = mFragBinding.rollCallTitleText.getText().toString();
    String description = mFragBinding.rollCallEventDescriptionText.getText().toString();
    Single<String> createRollCall =
        viewModel.createNewRollCall(
            title, description, creationTimeInSeconds, startTimeInSeconds, endTimeInSeconds);

    if (open) {
      viewModel.addDisposable(
          createRollCall
              .flatMapCompletable(viewModel::openRollCall)
              .subscribe(
                  // Open the scanning fragment when everything is done
                  () -> {
                    setCurrentFragment(
                        getParentFragmentManager(),
                        R.id.add_attendee_layout,
                        QRCodeScanningFragment::new);
                    viewModel.setPageTitle(getString(R.string.add_attendee_title));
                  },
                  error ->
                      ErrorUtils.logAndShow(
                          requireContext(), TAG, error, R.string.error_create_rollcall)));
    } else {
      viewModel.addDisposable(
          createRollCall.subscribe(
              id -> {
                setCurrentFragment(
                    getParentFragmentManager(),
                    R.id.fragment_lao_detail,
                    LaoDetailFragment::newInstance);
                viewModel.setPageTitle(viewModel.getLaoView().getName());
              },
              error ->
                  ErrorUtils.logAndShow(
                      requireContext(), TAG, error, R.string.error_create_rollcall)));
    }
  }

}
