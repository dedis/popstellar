package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where you can have the receipt a coin */
@AndroidEntryPoint
public class DigitalCashReceiptFragment extends Fragment {
    private DigitalCashSendFragmentBinding mDigitalCashSendFragBinding;
    private DigitalCashViewModel mDigitalCashViewModel;
    private

    public DigitalCashReceiptFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DigitalCashReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DigitalCashReceiveFragment newInstance() {
        DigitalCashReceiveFragment fragment = new DigitalCashReceiveFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.digital_cash_receive_fragment, container, false);
    }
}