package com.github.dedis.student20_pop.detail.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.dedis.student20_pop.databinding.FragmentSetupElectionEventBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ElectionSetupFragment extends AbstractEventCreationFragment implements AdapterView.OnItemSelectedListener {

    public static final String TAG = ElectionSetupFragment.class.getSimpleName();

    private FragmentSetupElectionEventBinding mSetupElectionFragBinding;

    //mandatory fields for submitting
    private EditText electionNameText;
    private EditText electionQuestionText;
    private Button submitButton;

    private enum votingMethods {Plurality}
    private votingMethods votingMethod;

    private List<String> ballotOptions;

    //the number of valid ballot options set by the organizer
    private int numberBallotOptions = 0;

    //Text watcher that checks if mandatory fields are filled for submitting each time the user changes a field (with at least two valid ballot options)
    private final TextWatcher submitTextWatcher =
            new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {/* no check to make before text is changed */}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {/* no check to make during the text is being changes */}

                @Override
                public void afterTextChanged(Editable s) {
                    boolean areFieldsFilled =
                        !electionNameText.getText().toString().trim().isEmpty() && !getStartDate().isEmpty() && !getStartTime().isEmpty() && !getEndDate().isEmpty() && !getEndTime().isEmpty() &&
                                !electionQuestionText.getText().toString().trim().isEmpty() && numberBallotOptions >= 2;
                    submitButton.setEnabled(areFieldsFilled);}
            };
    //Text watcher that adds a ballot option to the list when user finishes writing in the ballot options fields (and counts it as a valid option for submitting)
    private class BallotOptionsTextWatcher implements TextWatcher {

        // If the user changes the text of the same ballot option, its name in the list will be changed as well. Hence, we have to keep track of the ballot option's index
        private int ballotIndex;

        public BallotOptionsTextWatcher(int ballotIndex) {
            this.ballotIndex = ballotIndex;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {/* no check to make before text is changed */}

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) { /*no check to make during the text is being changed */}

        @Override
        public void afterTextChanged(Editable editable) {
            //If the list of ballot options was initially empty, or if the text is empty, then we count it as a valid ballot option
            if (ballotOptions.isEmpty() || (ballotOptions.get(ballotIndex).isEmpty() && !editable.toString().isEmpty())) numberBallotOptions++;
            // If the text was not empty, and we modify it to make it empty, then we remove it from the count of valid ballot options
            else if (!ballotOptions.get(ballotIndex).isEmpty() && editable.toString().isEmpty())  numberBallotOptions--;
            // Either way, we save our change in the corresponding index
            ballotOptions.set(ballotIndex, editable.toString());
        }
    }

    public static ElectionSetupFragment newInstance() {
        return new ElectionSetupFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        mSetupElectionFragBinding =
                FragmentSetupElectionEventBinding.inflate(inflater, container, false);

        LaoDetailViewModel laoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

        setDateAndTimeView(mSetupElectionFragBinding.getRoot(), this, getFragmentManager());
        addDateAndTimeListener(submitTextWatcher);

        Button cancelButton = mSetupElectionFragBinding.electionCancelButton;

        ballotOptions = new ArrayList<>();

        submitButton = mSetupElectionFragBinding.electionSubmitButton;
        electionNameText = mSetupElectionFragBinding.electionSetupTitle;
        electionQuestionText = mSetupElectionFragBinding.electionQuestion;

        //Add text watchers on the fields that need to be filled
        electionQuestionText.addTextChangedListener(submitTextWatcher);
        electionNameText.addTextChangedListener(submitTextWatcher);

        // Set up the basic fields for ballot options, with at least two options
        initNewBallotOptionsField();

        //When the button is clicked, add a new ballot option
       FloatingActionButton addBallotOptionButton = mSetupElectionFragBinding.addBallotOption;
        addBallotOptionButton.setOnClickListener(
                v -> addBallotOption());


        // Set the text widget in layout to current LAO name
        TextView laoNameTextView = mSetupElectionFragBinding.electionSetupLaoName;
        laoNameTextView.setText(laoDetailViewModel.getCurrentLaoName().getValue());

        //Set dropdown spinner as a list of string (from enum of votingmethods)
       Spinner spinner = mSetupElectionFragBinding.electionSetupSpinner;
        String[] items = Arrays.stream(votingMethods.values()).map(votingMethods::toString).toArray(String[]::new);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        //On click, submit button creates a new election event and launches the election UI for organizer
       submitButton.setOnClickListener(
                v -> {
                    computeTimesInSeconds();
                    String title = electionNameText.getText().toString();
                    String question = electionQuestionText.getText().toString();
                    List<String> filteredBallotOptions = new ArrayList<>();
                    for (String ballotOption: ballotOptions) {
                        if (!ballotOption.equals("")) filteredBallotOptions.add(ballotOption);
                    }
                    laoDetailViewModel.createNewElection(title, startTimeInSeconds, endTimeInSeconds, votingMethod.toString(), mSetupElectionFragBinding.writeIn.isChecked(), filteredBallotOptions, question);
                });

        //On click, cancel button takes back to LAO detail page
        cancelButton.setOnClickListener(
                v ->
                    laoDetailViewModel.openLaoDetail());
        mSetupElectionFragBinding.setLifecycleOwner(getActivity());

        return mSetupElectionFragBinding.getRoot();

    }

    // Spinner methods
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String item = adapterView.getItemAtPosition(i).toString();
        votingMethod = votingMethods.valueOf(item);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        votingMethod = votingMethods.valueOf("Plurality");
    }

    private EditText addBallotOption() {
        Context c = getActivity();
        EditText ballotOption = new EditText(c);
        ballotOption.setHint("ballot option");
        mSetupElectionFragBinding.electionSetupFieldsLl.addView(ballotOption);
        ViewGroup.LayoutParams params = ballotOption.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        ballotOption.setLayoutParams(params);
        int ballotTag = ballotOptions.size();
        ballotOption.setTag(ballotTag);
        ballotOption.addTextChangedListener(new BallotOptionsTextWatcher(ballotTag));
        ballotOption.addTextChangedListener(submitTextWatcher);
        ballotOptions.add(ballotOption.getText().toString());
        return ballotOption;
    }
    //By default, always at least two ballot options
    private void initNewBallotOptionsField() {
        addBallotOption();
        addBallotOption();
    }

}
