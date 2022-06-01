package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashSendFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashSendFragment extends Fragment {
    private DigitalCashSendFragmentBinding mBinding;
    private DigitalCashViewModel mViewModel;

    public DigitalCashSendFragment() {
        // not implemented yet
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashSendFragment.
     */
    public static DigitalCashSendFragment newInstance() {
        return new DigitalCashSendFragment();
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewModel = DigitalCashMain.obtainViewModel(getActivity());
        mBinding = DigitalCashSendFragmentBinding.inflate(inflater, container, false);

        // Inflate the layout for this fragment
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.digitalCashSendSend.setOnClickListener(
                clicked -> mViewModel.openReceipt()
        );
    }
}
