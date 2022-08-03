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

  private LaunchFragmentBinding mLaunchFragBinding;
  private HomeViewModel mHomeViewModel;

  public static LaunchFragment newInstance() {
    return new LaunchFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    mLaunchFragBinding = LaunchFragmentBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(requireActivity());

    mLaunchFragBinding.setViewModel(mHomeViewModel);
    mLaunchFragBinding.setLifecycleOwner(getActivity());

    return mLaunchFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupLaunchButton();
    setupCancelButton();
  }

  private void setupLaunchButton() {
    mLaunchFragBinding.buttonLaunch.setOnClickListener(
        v -> mHomeViewModel.launchLao(requireActivity()));
  }

  private void setupCancelButton() {
    mLaunchFragBinding.buttonCancelLaunch.setOnClickListener(
        v -> {
          mLaunchFragBinding.entryBoxLaunch.getText().clear();
          mHomeViewModel.setCurrentTab(HomeTab.HOME);
        });
  }
}
