package com.github.dedis.popstellar.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaunchFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the Launch UI */
@AndroidEntryPoint
public final class LaunchFragment extends Fragment {

  public static final String TAG = LaunchFragment.class.getSimpleName();

  private LaunchFragmentBinding binding;
  private HomeViewModel viewModel;

  public static LaunchFragment newInstance() {
    return new LaunchFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = LaunchFragmentBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(getActivity());
    viewModel = HomeActivity.obtainViewModel(requireActivity());

    setupLaunchButton();
    setupCancelButton();
    setupTextFieldWatcher();

    return binding.getRoot();
  }

  TextWatcher launchWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
          // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
          String laoName = binding.laoNameEntry.getEditText().getText().toString().trim();
          String serverUrl = binding.serverUrlEntry.getEditText().getText().toString().trim();
          boolean areFieldsFilled = !laoName.isEmpty() && serverUrl.isEmpty();
          binding.buttonLaunch.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable editable) {
          // Do nothing
        }
      };

  private void setupTextFieldWatcher() {
    binding.serverUrlEntry.getEditText().addTextChangedListener(launchWatcher);
    binding.laoNameEntry.getEditText().addTextChangedListener(launchWatcher);
  }

  private void setupLaunchButton() {
    binding.buttonLaunch.setOnClickListener(
        v -> {
          Context ctx = requireContext();
          viewModel.addDisposable(
              viewModel
                  .launchLao(binding.laoNameEntry.getEditText().getText().toString())
                  .subscribe(
                      laoId -> {
                        Log.d(TAG, "Opening lao detail activity on the home tab for lao " + laoId);
                        startActivity(LaoDetailActivity.newIntentForLao(ctx, laoId));
                      },
                      error -> ErrorUtils.logAndShow(ctx, TAG, error, R.string.error_create_lao)));
        });
  }

  private void setupCancelButton() {
    binding.buttonCancelLaunch.setOnClickListener(
        v -> {
          binding.laoNameEntry.getEditText().getText().clear();
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);
        });
  }
}
