package com.github.dedis.popstellar.ui.home.connecting;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.databinding.ConnectingFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass. Use the {@link ConnectingFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
@AndroidEntryPoint
public final class ConnectingFragment extends Fragment {

  public static final String TAG = ConnectingFragment.class.getSimpleName();

  private ConnectingFragmentBinding mConnectingFragBinding;
  private ConnectingViewModel mConnectingViewModel;

  /** Create a new instance of the connecting fragment. */
  public static ConnectingFragment newInstance() {
    return new ConnectingFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    mConnectingFragBinding = ConnectingFragmentBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof ConnectingActivity) {
      mConnectingViewModel = ConnectingActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
    }

    mConnectingFragBinding.setViewModel(mConnectingViewModel);
    mConnectingFragBinding.setLifecycleOwner(activity);

    return mConnectingFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupCancelButton();

    // Subscribe to "cancel LAO connect" event
    mConnectingViewModel
        .getCancelConnectEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                cancelConnect();
              }
            });
  }

  @Override
  public void onStop() {
    super.onStop();
    cancelConnect();
  }

  private void setupCancelButton() {
    mConnectingFragBinding.buttonCancelConnecting.setOnClickListener(
        v -> mConnectingViewModel.cancelConnect());
  }

  private void cancelConnect() {
    mConnectingViewModel.openHome();
  }
}
