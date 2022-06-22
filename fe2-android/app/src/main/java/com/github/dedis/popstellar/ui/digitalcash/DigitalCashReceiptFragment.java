package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.DigitalCashReceiptFragmentBinding;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashReceiptFragment} factory method to
 * create an instance of this fragment.
 */
public class DigitalCashReceiptFragment extends Fragment {
    private DigitalCashReceiptFragmentBinding mBinding;
    private DigitalCashViewModel mViewModel;

    public DigitalCashReceiptFragment() {
        // not implemented yet
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashReceiveFragment.
     */
    public static DigitalCashReceiptFragment newInstance() {
        return new DigitalCashReceiptFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mViewModel = DigitalCashActivity.obtainViewModel(getActivity());
        mBinding = DigitalCashReceiptFragmentBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel
                .getUpdateReceiptAmountEvent()
                .observe(
                        getViewLifecycleOwner(),
                        stringEvent -> {
                            String amount = stringEvent.getContentIfNotHandled();
                            if (amount != null) {
                                mBinding.digitalCashReceiptAmount.setText(amount);
                            }
                        });
        mViewModel
                .getUpdateReceiptAddressEvent()
                .observe(
                        getViewLifecycleOwner(),
                        stringEvent -> {
                            String address = stringEvent.getContentIfNotHandled();
                            if (address != null) {
                                mBinding.digitalCashReceiptBeneficiary.setText(
                                        String.format("Beneficary : %n %s", address));
                            }
                        });
    }
}
