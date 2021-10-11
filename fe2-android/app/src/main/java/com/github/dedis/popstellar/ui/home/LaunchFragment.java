package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.LaunchFragmentBinding;

/** Fragment used to display the Launch UI */
public final class LaunchFragment extends Fragment {

  public static final String TAG = LaunchFragment.class.getSimpleName();

  private LaunchFragmentBinding mLaunchFragBinding;
  private HomeViewModel mHomeViewModel;

  public static LaunchFragment newInstance() {
    return new LaunchFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    mLaunchFragBinding = LaunchFragmentBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(getActivity());

    mLaunchFragBinding.setViewModel(mHomeViewModel);
    mLaunchFragBinding.setLifecycleOwner(getActivity());

    return mLaunchFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupLaunchButton();
    setupCancelButton();

    // Subscribe to "launch LAO" event
    mHomeViewModel
        .getLaunchNewLaoEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                launchLao();
              }
            });

    // Subscribe to "cancel launch" event
    mHomeViewModel
        .getCancelNewLaoEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                cancelLaoLaunch();
              }
            });
  }

  private void setupLaunchButton() {
    mLaunchFragBinding.buttonLaunch.setOnClickListener(v -> mHomeViewModel.launchNewLao());
  }

  private void setupCancelButton() {
    mLaunchFragBinding.buttonCancelLaunch.setOnClickListener(v -> mHomeViewModel.cancelNewLao());
  }

  private void launchLao() {
    mHomeViewModel.launchLao();
    // mHomeViewModel.openHome();
  }

  private void cancelLaoLaunch() {
    mLaunchFragBinding.entryBoxLaunch.getText().clear();
    mHomeViewModel.openHome();
  }
}
