package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.dedis.popstellar.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DigitalCashSend#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DigitalCashSend extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG = "param";

    public DigitalCashSend() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View send = inflater.inflate(R.layout.fragment_digital_cash_send, container, false);
        Bundle args = getArguments();
        String param = Integer.toString(args.getInt(DigitalCashSend.ARG));
        TextView msg_dummie = (TextView)  send.findViewById(R.id.dummie_msg);
        msg_dummie.setText(param);
        return send;
    }
}