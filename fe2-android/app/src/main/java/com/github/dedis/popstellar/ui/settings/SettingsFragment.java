package com.github.dedis.popstellar.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.SettingsFragmentBinding;

public class SettingsFragment extends Fragment {

  private final String TAG = SettingsFragment.class.getSimpleName();

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

    mSettingsViewModel = SettingsActivity.obtainViewModel(getActivity());

    mSettingsFragBinding.setViewmodel(mSettingsViewModel);
    mSettingsFragBinding.setLifecycleOwner(getActivity());

    return mSettingsFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupApplyButton();

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
    // LAORequestFactory.setUrl(mSettingsFragBinding.entryBoxServerUrl.getText());
  }
}
