package com.github.dedis.student20_pop.ui.event;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.model.event.RollCallEvent;
import com.github.dedis.student20_pop.utility.ui.organizer.OnAddAttendeesListener;
import com.github.dedis.student20_pop.utility.ui.organizer.OnEventCreatedListener;

import java.util.ArrayList;
import java.util.List;

public class RollCallEventCreationFragment extends AbstractEventCreationFragment {
    public static final String TAG = RollCallEventCreationFragment.class.getSimpleName();

    private EditText rollCallDescriptionEditText;
    private EditText rollCallTitleEditText;
    private RollCallEvent rollCallEvent;


    private Button confirmButton;
    private final TextWatcher confirmTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String meetingTitle = rollCallTitleEditText.getText().toString().trim();

            confirmButton.setEnabled(!meetingTitle.isEmpty() &&
                    !getStartDate().isEmpty() &&
                    !getStartTime().isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    private Button openButton;
    private OnEventCreatedListener eventCreatedListener;
    private OnAddAttendeesListener onAddAttendeesListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        eventCreatedListener = (OnEventCreatedListener) context;
        onAddAttendeesListener = (OnAddAttendeesListener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentManager fragmentManager = (getActivity()).getSupportFragmentManager();
        View view = inflater.inflate(R.layout.fragment_create_roll_call_event, container, false);
        PoPApplication app = (PoPApplication) getActivity().getApplication();

        setDateAndTimeView(view, RollCallEventCreationFragment.this, fragmentManager);
        addDateAndTimeListener(confirmTextWatcher);

        rollCallTitleEditText = view.findViewById(R.id.roll_call_title_text);
        rollCallDescriptionEditText = view.findViewById(R.id.roll_call_event_description_text);

        openButton = view.findViewById(R.id.roll_call_open);


        confirmButton = view.findViewById(R.id.roll_call_confirm);
        confirmButton.setOnClickListener(v -> {
            new RollCallEvent(
                    rollCallTitleEditText.getText().toString(),
                    startDate,
                    endDate,
                    startTime,
                    endTime,
                    app.getCurrentLao().getId(),
                    NO_LOCATION,
                    rollCallDescriptionEditText.getText().toString(),
                    new ArrayList<>()
            );

            eventCreatedListener.OnEventCreatedListener(rollCallEvent);

            fragmentManager.popBackStackImmediate();
        });

        openButton.setOnClickListener(v -> {
            onAddAttendeesListener.onAddAttendeesListener(rollCallEvent.getId());
        });

        Button cancelButton = view.findViewById(R.id.roll_call_cancel);
        cancelButton.setOnClickListener(v -> {
            fragmentManager.popBackStackImmediate();
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkDates(requestCode, resultCode, data);
    }
}
