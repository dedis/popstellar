package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;

import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.ADD_ROLL_CALL;

public class AddAttendeeFragment extends Fragment {

    public static final String TAG = AddAttendeeFragment.class.getSimpleName();
    private final String eventId;

    public AddAttendeeFragment(String eventId){
        super();
        this.eventId = eventId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_attendee, container, false);
        Fragment newFragment = new QRCodeScanningFragment(ADD_ROLL_CALL, eventId);
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.add_attendee_qr_code_fragment, newFragment, QRCodeScanningFragment.TAG).addToBackStack(null).commit();
        return view;
    }
}
