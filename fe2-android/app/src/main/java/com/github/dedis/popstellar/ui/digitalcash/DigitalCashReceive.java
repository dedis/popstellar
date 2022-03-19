package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.dedis.popstellar.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
@AndroidEntryPoint
public class DigitalCashReceive extends Fragment {

    //private final DigitalCashViewModel;
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG = "param";

    public DigitalCashReceive() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //Bundle args = getArguments();
        return inflater.inflate(R.layout.fragment_digital_cash_receive, container, false);
    }
}