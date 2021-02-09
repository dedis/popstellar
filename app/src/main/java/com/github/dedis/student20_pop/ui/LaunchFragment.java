package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.student20_pop.Event;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ViewModelFactory;
import com.github.dedis.student20_pop.launch.LaunchViewModel;
import com.github.dedis.student20_pop.utility.ActivityUtils;

/** Fragment used to display the Launch UI */
public final class LaunchFragment extends Fragment {

  public static final String TAG = LaunchFragment.class.getSimpleName();

  private LaunchViewModel mViewModel;

  public static LaunchFragment newInstance() {
    return new LaunchFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater,
          @Nullable ViewGroup container,
          @Nullable Bundle savedInstanceState) {

    mViewModel = obtainViewModel(this);

    // Subscribe to "launch LAO" event
    mViewModel.getLaunchLaoEvent().observe(this, new Observer<Event<Boolean>>() {
      @Override
      public void onChanged(Event<Boolean> booleanEvent) {
        Boolean action = booleanEvent.getContentIfNotHandled();
        if (action != null) {
          launchLao();
        }
      }
    });

    // Subscribe to "cancel launch" event
    mViewModel.getCancelLaunchEvent().observe(this, new Observer<Event<Boolean>>() {
      @Override
      public void onChanged(Event<Boolean> booleanEvent) {
        Boolean action = booleanEvent.getContentIfNotHandled();
        if (action != null) {
          setupHomeFragment();
        }
      }
    });

    return inflater.inflate(R.layout.fragment_launch, container, false);
  }

  public static LaunchViewModel obtainViewModel(Fragment fragment) {
    ViewModelFactory factory = ViewModelFactory.getInstance(fragment.getActivity().getApplication());
    LaunchViewModel viewModel = new ViewModelProvider(fragment, factory).get(LaunchViewModel.class);

    return viewModel;
  }

  private void launchLao() {
    // get text from edit text
    // create lao
  }

  private void setupHomeFragment() {
    // simulate click open home
    HomeFragment homeFragment = (HomeFragment) getActivity().getSupportFragmentManager()
            .findFragmentById(R.id.fragment_container_main);
    if (homeFragment == null) {
      homeFragment = HomeFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
              getActivity().getSupportFragmentManager(), homeFragment, R.id.fragment_container_main
      );
    }
  }
}