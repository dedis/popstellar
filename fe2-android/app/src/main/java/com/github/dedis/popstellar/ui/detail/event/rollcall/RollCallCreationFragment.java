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
import com.github.dedis.popstellar.ui.detail.event.AbstractEventCreationFragment;
import com.github.dedis.popstellar.ui.detail.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;

/** Fragment that shows up when user wants to create a Roll-Call Event */
@AndroidEntryPoint
public final class RollCallCreationFragment extends AbstractEventCreationFragment {

  public static final String TAG = RollCallCreationFragment.class.getSimpleName();

  private RollCallCreateFragmentBinding binding;
  private LaoViewModel viewModel;
  private RollCallViewModel rollCallViewModel;
  private EditText rollCallTitleEditText;
  private Button confirmButton;

  private final TextWatcher confirmTextWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          // Nothing needed here
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          String rcTitle = rollCallTitleEditText.getText().toString().trim();
          String location = binding.rollCallEventLocationText.getText().toString().trim();
          boolean areFieldsFilled =
              !rcTitle.isEmpty()
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

  public static RollCallCreationFragment newInstance() {
    return new RollCallCreationFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = RollCallCreateFragmentBinding.inflate(inflater, container, false);

    viewModel = LaoActivity.obtainViewModel(requireActivity());
    rollCallViewModel =
        LaoActivity.obtainRollCallViewModel(requireActivity(), viewModel.getLaoId());

    setDateAndTimeView(binding.getRoot());
    addStartDateAndTimeListener(confirmTextWatcher);

    rollCallTitleEditText = binding.rollCallTitleText;
    rollCallTitleEditText.addTextChangedListener(confirmTextWatcher);
    binding.rollCallEventLocationText.addTextChangedListener(confirmTextWatcher);

    confirmButton = binding.rollCallConfirm;
    confirmButton.setEnabled(false);

    binding.setLifecycleOwner(getActivity());

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
    viewModel.setPageTitle(R.string.roll_call_setup_title);
    viewModel.setIsTab(false);
  }

  private void setupConfirmButton() {
    confirmButton.setOnClickListener(v -> createRollCall());
  }

  private void createRollCall() {
    if (!computeTimesInSeconds()) {
      return;
    }

    String title = Objects.requireNonNull(binding.rollCallTitleText.getText()).toString();
    String description =
        Objects.requireNonNull(binding.rollCallEventDescriptionText.getText()).toString();
    String location =
        Objects.requireNonNull(binding.rollCallEventLocationText.getText().toString());
    Single<String> createRollCall =
        rollCallViewModel.createNewRollCall(
            title,
            description,
            location,
            creationTimeInSeconds,
            startTimeInSeconds,
            endTimeInSeconds);

    viewModel.addDisposable(
        createRollCall.subscribe(
            id ->
                LaoActivity.setCurrentFragment(
                    getParentFragmentManager(),
                    R.id.fragment_event_list,
                    EventListFragment::newInstance),
            error ->
                ErrorUtils.logAndShow(
                    requireContext(), TAG, error, R.string.error_create_rollcall)));
  }
}
