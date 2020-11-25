package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;

/**
 * Fragment used to display the Home UI
**/
public final class HomeFragment extends Fragment {

    public static final String TAG = HomeFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO: retrieve list of LAOs from backend and display them

        return inflater.inflate(R.layout.fragment_home, container, false);
    }
}
