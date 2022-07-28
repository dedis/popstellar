package com.github.dedis.popstellar.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SettingsFragmentBinding;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.home.HomeActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

  private final String TAG = SettingsFragment.class.getSimpleName();

  @Inject GlobalNetworkManager networkManager;

  private SettingsFragmentBinding mSettingsFragBinding;
  private SettingsViewModel mSettingsViewModel;

  public static SettingsFragment newInstance() {
    return new SettingsFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mSettingsFragBinding = SettingsFragmentBinding.inflate(inflater, container, false);

    mSettingsViewModel = SettingsActivity.obtainViewModel(requireActivity());

    mSettingsFragBinding.setViewmodel(mSettingsViewModel);
    mSettingsFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mSettingsFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupApplyButton();
    mSettingsViewModel.setServerUrl(networkManager.getCurrentUrl());
    mSettingsViewModel.setCheckServerUrl(networkManager.getCurrentUrl());

    // Subscribe to "apply changes" event
    mSettingsViewModel
        .getApplyChangesEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                applyChanges();
              }
            });
  }

  private void setupApplyButton() {
    mSettingsFragBinding.buttonApply.setOnClickListener(v -> mSettingsViewModel.applyChanges());
  }

  private void applyChanges() {
    networkManager.connect(mSettingsFragBinding.entryBoxServerUrl.getText().toString());
    Intent intent = new Intent(getActivity(), HomeActivity.class);
    Log.d(TAG, "Trying to open home");
    startActivity(intent);
  }
}
