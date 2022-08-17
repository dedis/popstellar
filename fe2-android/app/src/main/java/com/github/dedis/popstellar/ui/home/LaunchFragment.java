package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.LaunchFragmentBinding;

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

    return binding.getRoot();
  }

  private void setupLaunchButton() {
    binding.buttonLaunch.setOnClickListener(
        v -> viewModel.launchLao(requireActivity(), binding.laoNameEntry.getText().toString()));
  }

  private void setupCancelButton() {
    binding.buttonCancelLaunch.setOnClickListener(
        v -> {
          binding.laoNameEntry.getText().clear();
          viewModel.setCurrentTab(HomeTab.HOME);
        });
  }
}
