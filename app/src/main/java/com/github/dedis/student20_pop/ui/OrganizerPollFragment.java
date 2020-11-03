package com.github.dedis.student20_pop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Event;
import com.github.dedis.student20_pop.utility.pollUI.ChoicesListViewAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public final class OrganizerPollFragment extends Fragment {
    public static final String TAG = OrganizerPollFragment.class.getSimpleName();
    private EditText questionEditText;
    private CheckBox nowCheckBox;
    private EditText startDateEditText;
    private EditText endDateEditText;
    private EditText startTimeEditText;
    private EditText endTimeEditText;
    private RadioButton pollType1;
    private RadioButton pollType2;
    private ListView choicesListView;
    private Button scheduleButton;
    private Button openButton;
    private Button cancelButton;
    private Date today = Calendar.getInstance().getTime();
    private String question;
    private ChoicesListViewAdapter listViewAdapter;
    private LinkedList<Integer> choices;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_poll, container, false);
        questionEditText = view.findViewById(R.id.question);
        nowCheckBox = view.findViewById(R.id.now_check_box);
        nowCheckBox.setChecked(true);
        startDateEditText = view.findViewById(R.id.start_date_edit_text);
        startDateEditText.setVisibility(View.GONE);
        startTimeEditText = view.findViewById(R.id.start_time_edit_text);
        startTimeEditText.setVisibility(View.GONE);
        endDateEditText = view.findViewById(R.id.end_date_edit_text);
        endTimeEditText = view.findViewById(R.id.end_time_edit_text);
        pollType1 = view.findViewById(R.id.radio_poll_type_1);
        pollType1.setChecked(true);
        pollType2 = view.findViewById(R.id.radio_poll_type_2);
        choices = new LinkedList<>();
        choices.add(1);
        choices.add(2);
        choicesListView = view.findViewById(R.id.choices_list);
        listViewAdapter = new ChoicesListViewAdapter(this.getContext(), choices);
        choicesListView.setAdapter(listViewAdapter);
        scheduleButton = view.findViewById(R.id.schedule_button);
        openButton = view.findViewById(R.id.open_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        nowCheckBox.setOnClickListener(clicked -> {
            if (startDateEditText.getVisibility() == View.GONE) {
                startDateEditText.setVisibility(View.VISIBLE);
                startTimeEditText.setVisibility(View.VISIBLE);
            }
            else {
                startDateEditText.setVisibility(View.GONE);
                startTimeEditText.setVisibility(View.GONE);
            }
        });

        //if one of choicesList's delete button is pressed --> need to update list
        //if one of choicesList's blank EditText is filled --> need to update list

        /*
        view.setOnSystemUiVisibilityChangeListener(viewed -> {
            scheduleButton.setClickable(isScheduleButtonEnabled());
            scheduleButton.setEnabled(isScheduleButtonEnabled());
            scheduleButton.setVisibility(setVisibleIfEnabled(isScheduleButtonEnabled()));
            openButton.setClickable(isOpenButtonEnabled());
            openButton.setEnabled(isOpenButtonEnabled());
            openButton.setVisibility(setVisibleIfEnabled(isOpenButtonEnabled()));
        });
         */

        scheduleButton.setOnClickListener(clicked -> {
            question = questionEditText.getText().toString();
        });

        openButton.setOnClickListener(clicked -> {
            question = questionEditText.getText().toString();
        });


        return view;

    }

    private boolean isScheduleButtonEnabled() {
        if (questionEditText.getText() != null && startDateEditText.getText() != null
                && startTimeEditText.getText() != null) { //&& choicesList.getChoiceNumber()>=2
            return true;
        } else {
            return false;
        }
    }

    private boolean isOpenButtonEnabled(){
        return nowCheckBox.isChecked(); //&& choicesList.getChoiceNumber()>=2
    }

    private int setVisibleIfEnabled(boolean enabled){
        if (enabled){
            return View.VISIBLE;
        } else{
            return View.INVISIBLE;
        }
    }
}
