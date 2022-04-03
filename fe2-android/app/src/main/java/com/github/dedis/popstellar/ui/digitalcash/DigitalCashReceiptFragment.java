package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashReceiptFragmentBinding;
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where you can have the receipt a coin */
@AndroidEntryPoint
public class DigitalCashReceiptFragment extends Fragment {
    private DigitalCashReceiptFragmentBinding mDigitalCashReceiptFragBinding;
    private DigitalCashViewModel mDigitalCashViewModel;

    public static DigitalCashReceiptFragment newInstance() {
        DigitalCashReceiptFragment fragment = new DigitalCashReceiptFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mDigitalCashReceiptFragBinding =
                DigitalCashReceiptFragmentBinding.inflate(inflater, container, false);

        mDigitalCashViewModel = DigitalCashMain.obtainViewModel(requireActivity());

        mDigitalCashReceiptFragBinding.setViewModel(mDigitalCashViewModel);
        mDigitalCashReceiptFragBinding.setLifecycleOwner(getViewLifecycleOwner());

        return mDigitalCashReceiptFragBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}