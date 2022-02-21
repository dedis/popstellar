package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.HomeFragmentBinding;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the Home UI */
@AndroidEntryPoint(Fragment.class)
public final class HomeFragment extends Hilt_HomeFragment {

  public static final String TAG = HomeFragment.class.getSimpleName();

  private HomeFragmentBinding mHomeFragBinding;
  private HomeViewModel mHomeViewModel;
  private LAOListAdapter mListAdapter;

  public static HomeFragment newInstance() {
    return new HomeFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mHomeFragBinding = HomeFragmentBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(requireActivity());

    mHomeFragBinding.setViewmodel(mHomeViewModel);
    mHomeFragBinding.setLifecycleOwner(getActivity());

    return mHomeFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupListAdapter();
    setupListUpdates();
  }

  private void setupListUpdates() {
    mHomeViewModel
        .getLAOs()
        .observe(
            requireActivity(),
            laos -> {
              Log.d(TAG, "Got a list update");

              mListAdapter.replaceList(laos);

              // TODO: perhaps move this to data binding
              if (laos.size() > 0) {
                mHomeFragBinding.welcomeScreen.setVisibility(View.GONE);
                mHomeFragBinding.listScreen.setVisibility(View.VISIBLE);
              }
            });
  }

  private void setupListAdapter() {
    ListView listView = mHomeFragBinding.laoList;

    mListAdapter = new LAOListAdapter(new ArrayList<>(0), mHomeViewModel, getActivity(), true);

    listView.setAdapter(mListAdapter);
  }
}
