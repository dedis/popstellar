package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.databinding.ServerUrlFragmentBinding;

public class ServerUrlFragment extends Fragment {

  public static final String TAG = ServerUrlFragment.class.getSimpleName();

  private ServerUrlFragmentBinding mServerUrlFragBinding;
  private HomeViewModel mHomeViewModel;

  public static ServerUrlFragment newInstance() {
    return new ServerUrlFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mServerUrlFragBinding = ServerUrlFragmentBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(getActivity());

    mServerUrlFragBinding.setViewModel(mHomeViewModel);
    mServerUrlFragBinding.setLifecycleOwner(getActivity());

    return mServerUrlFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupSetButton();
    mHomeViewModel.setServerUrl(Injection.getServerUrl());

    // Subscribe to "set server url" event
    mHomeViewModel
        .getSetServerUrlEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setServerUrl();
              }
            });
  }

  private void setupSetButton() {
    mServerUrlFragBinding.buttonServerUrl.setOnClickListener(
        v -> mHomeViewModel.setServerUrlEvent());
  }

  private void setServerUrl() {
    mHomeViewModel.serverUrl();
    mHomeViewModel.openHome();
  }
}
