package com.github.dedis.student20_pop.home.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.databinding.FragmentContentWalletBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.home.adapters.LAOListAdapter;

import java.util.ArrayList;

/** Fragment used to display the content wallet UI */
public class ContentWalletFragment extends Fragment {
  public static final String TAG = ContentWalletFragment.class.getSimpleName();
  public static ContentWalletFragment newInstance() {
    return new ContentWalletFragment();
  }

  private FragmentContentWalletBinding mContentWalletBinding;
  private HomeViewModel mHomeViewModel;
  private LAOListAdapter mListAdapter;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mContentWalletBinding = FragmentContentWalletBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(getActivity());

    mContentWalletBinding.setViewmodel(mHomeViewModel);
    mContentWalletBinding.setLifecycleOwner(getActivity());

    return mContentWalletBinding.getRoot();
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
                getActivity(),
                laos -> {
                  Log.d(TAG, "Got a list update");

                  mListAdapter.replaceList(laos);

                  if (!laos.isEmpty()) {
                    mContentWalletBinding.welcomeScreen.setVisibility(View.GONE);
                    mContentWalletBinding.listScreen.setVisibility(View.VISIBLE);
                  }
                });
  }

  private void setupListAdapter() {
    ListView listView = mContentWalletBinding.laoList;

    mListAdapter = new LAOListAdapter(new ArrayList<>(0), mHomeViewModel, getActivity(), false);

    listView.setAdapter(mListAdapter);
  }
}
