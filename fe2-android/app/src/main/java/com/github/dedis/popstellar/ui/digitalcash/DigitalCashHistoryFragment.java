package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashHistoryFragment#newInstance}
 * factory method to create an instance of this fragment.
 */
public class DigitalCashHistoryFragment extends Fragment {

    public DigitalCashHistoryFragment() {

    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashHistoryFragment.
     */
    public static DigitalCashHistoryFragment newInstance() {
        return new DigitalCashHistoryFragment();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.digital_cash_history_fragment, container, false);
    }
}
