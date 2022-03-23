package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DigitalCashIssueFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DigitalCashIssueFragment extends Fragment {

    public DigitalCashIssueFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DigitalCashIssueFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DigitalCashIssueFragment newInstance() {
        DigitalCashIssueFragment fragment = new DigitalCashIssueFragment();
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
        return inflater.inflate(R.layout.digital_cash_issue_fragment, container, false);
    }
}