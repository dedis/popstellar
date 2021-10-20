package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.databinding.ConnectingFragmentBinding;

/**
 * A simple {@link Fragment} subclass. Use the {@link ConnectingFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
public final class ConnectingFragment extends Fragment {

  public static final String TAG = ConnectingFragment.class.getSimpleName();

  private ConnectingFragmentBinding mConnectingFragBinding;
  private HomeViewModel mHomeViewModel;

  /** Create a new instance of the connecting fragment. */
  public static ConnectingFragment newInstance() {
    return new ConnectingFragment();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    mConnectingFragBinding = ConnectingFragmentBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mHomeViewModel = HomeActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("Cannot obtain view model for " + TAG);
    }

    mConnectingFragBinding.setViewModel(mHomeViewModel);
    mConnectingFragBinding.setLifecycleOwner(activity);

    return mConnectingFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupCancelButton();

    // Subscribe to "cancel LAO connect" event
    mHomeViewModel
        .getCancelConnectEvent()
        .observe(
            this,
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
        v -> mHomeViewModel.cancelConnect());
  }

  private void cancelConnect() {
    mHomeViewModel.openHome();
  }
}
