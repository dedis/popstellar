package com.github.dedis.student20_pop.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectingFragment extends Fragment {

    private static final String URL = "url";

    private String url;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param url to connect to
     * @return A new instance of fragment ConnectingFragment.
     */
    public static ConnectingFragment newInstance(String url) {
        ConnectingFragment fragment = new ConnectingFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_connecting, container, false);

        TextView url_view = view.findViewById(R.id.connecting_url);
        url_view.setText(url);

        return view;
    }
}