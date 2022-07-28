package com.github.dedis.popstellar.ui.wallet;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.LaoWalletFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoWalletFragment extends Fragment {

  public static final String TAG = LaoWalletFragment.class.getSimpleName();

  private LaoDetailViewModel mLaoDetailViewModel;
  private WalletListAdapter mWalletListAdapter;
  private LaoWalletFragmentBinding mLaoWalletFragmentBinding;

  public static LaoWalletFragment newInstance() {
    return new LaoWalletFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mLaoWalletFragmentBinding = LaoWalletFragmentBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    mLaoWalletFragmentBinding.setViewModel(mLaoDetailViewModel);
    mLaoWalletFragmentBinding.setLifecycleOwner(getActivity());

    return mLaoWalletFragmentBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupWalletListAdapter();
    setupWalletListUpdates();

    mLaoDetailViewModel
        .getLaoAttendedRollCalls()
        .observe(
            requireActivity(),
            rollCalls -> {
              Log.d(TAG, "Got a list update for LAO roll calls");
              mWalletListAdapter.replaceList(rollCalls);
            });

    mLaoWalletFragmentBinding.backButton.setOnClickListener(
        clicked -> mLaoDetailViewModel.openHome());
  }

  private void setupWalletListAdapter() {
    ListView listView = mLaoWalletFragmentBinding.walletList;

    mWalletListAdapter =
        new WalletListAdapter(new ArrayList<>(0), mLaoDetailViewModel, getActivity());

    listView.setAdapter(mWalletListAdapter);
  }

  private void setupWalletListUpdates() {
    mLaoDetailViewModel
        .getLaoAttendedRollCalls()
        .observe(
            requireActivity(),
            rollCalls -> {
              Log.d(TAG, "Got a wallet list update");
              mWalletListAdapter.replaceList(rollCalls);
            });
  }
}
