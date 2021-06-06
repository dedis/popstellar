package com.github.dedis.student20_pop.detail.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.databinding.FragmentLaoWalletBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.detail.adapters.WalletListAdapter;

import java.util.ArrayList;

public class LaoWalletFragment extends Fragment {
    public static final String TAG = LaoWalletFragment.class.getSimpleName();

    private LaoDetailViewModel mLaoDetailViewModel;
    private WalletListAdapter mWalletListAdapter;
    private FragmentLaoWalletBinding mFragmentLaoWalletBinding;

    public static LaoWalletFragment newInstance() {
        return new LaoWalletFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mFragmentLaoWalletBinding = FragmentLaoWalletBinding.inflate(inflater, container, false);

        mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

        mFragmentLaoWalletBinding.setViewModel(mLaoDetailViewModel);
        mFragmentLaoWalletBinding.setLifecycleOwner(getActivity());

        return mFragmentLaoWalletBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupPropertiesButton();
        setupWalletListAdapter();
        setupWalletListUpdates();

        mLaoDetailViewModel
                .getLaoAttendedRollCalls()
                .observe(
                        getActivity(),
                        rollCalls -> {
                            Log.d(TAG, "Got a list update for LAO roll calls");
                            mWalletListAdapter.replaceList(rollCalls);
                        });

        mFragmentLaoWalletBinding.backButton.setOnClickListener(clicked -> mLaoDetailViewModel.openHome());
    }

    private void setupPropertiesButton() {
        Button propertiesButton = (Button) getActivity().findViewById(R.id.tab_properties);

        propertiesButton.setOnClickListener(clicked -> mLaoDetailViewModel.toggleShowHideProperties());
    }

    private void setupWalletListAdapter() {
        ListView listView = mFragmentLaoWalletBinding.walletList;

        mWalletListAdapter = new WalletListAdapter(new ArrayList<>(0), mLaoDetailViewModel, getActivity());

        listView.setAdapter(mWalletListAdapter);
    }

    private void setupWalletListUpdates() {
        mLaoDetailViewModel
                .getLaoAttendedRollCalls()
                .observe(
                        getActivity(),
                        rollCalls -> {
                            Log.d(TAG, "Got a wallet list update");
                            mWalletListAdapter.replaceList(rollCalls);
                        }
                );
    }
}
