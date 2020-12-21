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
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.model.PollEvent;
import com.github.dedis.student20_pop.utility.ui.organizer.ChoicesListViewAdapter;
import com.github.dedis.student20_pop.utility.ui.organizer.OnEventCreatedListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PollEventCreationFragment extends AbstractEventCreationFragment {

    public static final String TAG = PollEventCreationFragment.class.getSimpleName();
    public static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.FRENCH);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

    private EditText questionEditText;

    private boolean pollTypeIsOneOfN;

    private Button scheduleButton;
    private Button cancelButton;

    private ListView choicesListView;
    private ChoicesListViewAdapter listViewAdapter;

    private final TextWatcher buttonsTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            scheduleButton.setEnabled(isScheduleButtonEnabled());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

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
        View view = inflater.inflate(R.layout.fragment_create_poll_event, container, false);

        setDateAndTimeView(view, PollEventCreationFragment.this, fragmentManager);
        addDateAndTimeListener(buttonsTextWatcher);

        //Question
        questionEditText = view.findViewById(R.id.question);
        questionEditText.addTextChangedListener(buttonsTextWatcher);

        //Radio Buttons: poll type
        RadioButton pollType1;
        pollType1 = view.findViewById(R.id.radio_poll_type_1);
        pollType1.setChecked(true);
        RadioGroup pollType = view.findViewById(R.id.radio_group_poll_type);
        pollType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                pollTypeIsOneOfN = (checkedId == R.id.radio_poll_type_1);
            }
        });

        //Choices list
        choicesListView = view.findViewById(R.id.choices_list);
        listViewAdapter = new ChoicesListViewAdapter(this.getContext());
        choicesListView.setAdapter(listViewAdapter);
        justifyListViewHeightBasedOnChildren(choicesListView);

        ImageButton addChoiceButton;
        addChoiceButton = view.findViewById(R.id.button_add);
        addChoiceButton.setFocusable(false);
        addChoiceButton.setOnClickListener(clicked -> {
            listViewAdapter.addChoice();
            justifyListViewHeightBasedOnChildren(choicesListView);
        });

        //NOT ON POINT YET
        //TODO pour que la list soit de meme taille que ce qu'on voit !!!
        choicesListView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                scheduleButton.setEnabled(isScheduleButtonEnabled());
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
                scheduleButton.setEnabled(isScheduleButtonEnabled());
            }
        });

        // formatting today's date
        try {
            today = DATE_FORMAT.parse(DATE_FORMAT.format(Calendar.getInstance().getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
            today = new Date();
        }

        // Schedule
        scheduleButton = view.findViewById(R.id.schedule_button);
        scheduleButton.setOnClickListener(clicked -> {
            PoPApplication app = (PoPApplication) (getActivity().getApplication());
            String question = questionEditText.getText().toString();
            ArrayList<String> choicesList = getChoices(choicesListView);
            Event pollEvent = new PollEvent(question,
                                            choicesList,
                                            pollTypeIsOneOfN,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime,
                                            app.getCurrentLao().getId(),
                                            "");
            eventCreatedListener.OnEventCreatedListener(pollEvent);

            fragmentManager.popBackStackImmediate();
        });

        cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(clicked -> {
            fragmentManager.popBackStackImmediate();
        });
        return view;


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkDates(requestCode, resultCode, data);
    }

    private boolean isScheduleButtonEnabled() {
        String question = questionEditText.getText().toString().trim();
        int numberOfChoices = getChoices(choicesListView).size();

        return !question.isEmpty() &&
                numberOfChoices >= 2;
    }
    
    public ArrayList<String> getChoices(ListView listView) {
        ArrayList<String> choices = new ArrayList<>();
        ChoicesListViewAdapter adapter = (ChoicesListViewAdapter) listView.getAdapter();
        if (adapter == null) {
            return choices;
        }
        int numberOfChoices = adapter.getCount();
        for (int i = 0; i < numberOfChoices; i++) {
            View v = listView.getChildAt(i);
            EditText choiceBox = v.findViewById(R.id.choice_edit_text);
            String choice = choiceBox.getText().toString();
            if (!choice.trim().isEmpty()) {
                choices.add(choice);
            }
        }
        return choices;
    }

    public static void justifyListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams par = listView.getLayoutParams();
        par.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(par);
        listView.requestLayout();
    }
}