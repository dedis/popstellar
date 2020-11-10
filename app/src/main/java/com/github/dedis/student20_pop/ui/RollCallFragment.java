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
 * Fragment used to display a Roll-Call
 **/
public final class RollCallFragment extends Fragment {

    public static final String TAG = RollCallFragment.class.getSimpleName();

    private Boolean creatingRollCall = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(creatingRollCall) {
            return inflater.inflate(R.layout.fragment_create_roll_call, container, false);
        }
        else {
            return inflater.inflate(R.layout.fragment_roll_call, container, false);
        }
    }

    /**
     * Set the state of the Roll-Call Fragment to create
     * @return the create Roll-Call Fragment
     */
    public RollCallFragment create() {
        this.creatingRollCall = true;
        return this;
    }

    public void onClick() {

    }
}