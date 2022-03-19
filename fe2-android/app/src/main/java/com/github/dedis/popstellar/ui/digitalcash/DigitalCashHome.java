package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DigitalCashHome#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DigitalCashHome extends Fragment {

    public static final String ARG = "param";

    public DigitalCashHome() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_digital_cash_home, container, false);
    }
}