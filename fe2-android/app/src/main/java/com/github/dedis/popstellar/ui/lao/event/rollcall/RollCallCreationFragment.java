package com.github.dedis.popstellar.ui.lao.event.rollcall;

import android.os.Bundle;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.RollCallCreateFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.AbstractEventCreationFragment;
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;

/** Fragment that shows up when user wants to create a Roll-Call Event */
@AndroidEntryPoint
public final class RollCallCreationFragment extends AbstractEventCreationFragment {

  public static final String TAG = RollCallCreationFragment.class.getSimpleName();

  private RollCallCreateFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private RollCallViewModel rollCallViewModel;
  private EditText rollCallTitleEditText;

  public static RollCallCreationFragment newInstance() {
    return new RollCallCreationFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = RollCallCreateFragmentBinding.inflate(inflater, container, false);

    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    rollCallViewModel =
        LaoActivity.obtainRollCallViewModel(requireActivity(), laoViewModel.getLaoId());

    confirmButton = binding.rollCallConfirm;
    confirmButton.setEnabled(false);

    TextWatcher confirmTextWatcher =
        getConfirmTextWatcher(rollCallTitleEditText, binding.rollCallEventLocationText);

    setDateAndTimeView(binding.getRoot());
    addStartDateAndTimeListener(confirmTextWatcher);

    rollCallTitleEditText = binding.rollCallTitleText;
    rollCallTitleEditText.addTextChangedListener(confirmTextWatcher);
    binding.rollCallEventLocationText.addTextChangedListener(confirmTextWatcher);

    binding.setLifecycleOwner(getActivity());

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.roll_call_setup_title);
    laoViewModel.setIsTab(false);
  }

  @Override
  protected void createEvent() {
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

    laoViewModel.addDisposable(
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

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG, "event list", () -> EventListFragment.openFragment(getParentFragmentManager())));
  }
}
