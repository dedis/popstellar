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
import com.github.dedis.student20_pop.model.event.MeetingEvent;
import com.github.dedis.student20_pop.utility.ui.organizer.OnEventCreatedListener;

public final class MeetingEventCreationFragment extends AbstractEventCreationFragment {
    public static final String TAG = MeetingEventCreationFragment.class.getSimpleName();

    private EditText meetingTitleEditText;
    private EditText meetingLocationEditText;
    private EditText meetingDescriptionEditText;

    private Button confirmButton;
    private final TextWatcher confirmTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String meetingTitle = meetingTitleEditText.getText().toString().trim();

            confirmButton.setEnabled(!meetingTitle.isEmpty() &&
                    !getStartDate().isEmpty() &&
                    !getStartTime().isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private Button cancelButton;
    private OnEventCreatedListener eventCreatedListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEventCreatedListener)
            eventCreatedListener = (OnEventCreatedListener) context;
        else
            throw new ClassCastException(context.toString() + " must implement OnEventCreatedListener");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final FragmentManager fragmentManager = (getActivity()).getSupportFragmentManager();
        View view = inflater.inflate(R.layout.fragment_create_meeting_event, container, false);
        PoPApplication app = (PoPApplication) getActivity().getApplication();

        setDateAndTimeView(view, MeetingEventCreationFragment.this, fragmentManager);
        addDateAndTimeListener(confirmTextWatcher);


        meetingTitleEditText = view.findViewById(R.id.title_text);
        meetingLocationEditText = view.findViewById(R.id.meeting_event_location_text);
        meetingDescriptionEditText = view.findViewById(R.id.meeting_event_description_text);

        meetingTitleEditText.addTextChangedListener(confirmTextWatcher);

        confirmButton = view.findViewById(R.id.confirm);
        confirmButton.setOnClickListener(v -> {

            Event meetingEvent = new MeetingEvent(
                    meetingTitleEditText.getText().toString(),
                    startDate,
                    endDate,
                    startTime,
                    endTime,
                    app.getCurrentLao().getId(),
                    meetingLocationEditText.getText().toString(),
                    meetingDescriptionEditText.getText().toString());

            eventCreatedListener.OnEventCreatedListener(meetingEvent);

            fragmentManager.popBackStackImmediate();
        });

        cancelButton = view.findViewById(R.id.cancel);
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
