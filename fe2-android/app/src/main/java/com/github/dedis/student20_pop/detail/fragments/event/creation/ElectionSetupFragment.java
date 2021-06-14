package com.github.dedis.student20_pop.detail.fragments.event.creation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.student20_pop.databinding.FragmentSetupElectionEventBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.detail.adapters.ElectionSetupViewPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ElectionSetupFragment extends AbstractEventCreationFragment{

    public static final String TAG = ElectionSetupFragment.class.getSimpleName();

    private FragmentSetupElectionEventBinding mSetupElectionFragBinding;

    //mandatory fields for submitting
    private EditText electionNameText;
    private Button submitButton;
    private ElectionSetupViewPagerAdapter viewPagerAdapter;
    private LaoDetailViewModel mLaoDetailViewModel;

    //Enum of all voting methods, associated to a string desc for protocol and spinner display
    public enum VotingMethods { PLURALITY("Plurality");
        private String desc;
        VotingMethods(String desc) { this.desc=desc; }
        public String getDesc() { return desc; }
    }



    //the number of valid ballot options set by the organizer

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
                            !electionNameText.getText().toString().trim().isEmpty() && !getStartDate().isEmpty() && !getStartTime().isEmpty() && !getEndDate().isEmpty() && !getEndTime().isEmpty()
                                    && viewPagerAdapter.isAnInputValid() ;
                    submitButton.setEnabled(areFieldsFilled);}
            };

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

        mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());



        //Set the view for the date and time
        setDateAndTimeView(mSetupElectionFragBinding.getRoot(), this, getFragmentManager());
        //Make the textWatcher listen to changes in the start and end date/time
        addEndDateAndTimeListener(submitTextWatcher);
        addStartDateAndTimeListener(submitTextWatcher);


        submitButton = mSetupElectionFragBinding.electionSubmitButton;
        electionNameText = mSetupElectionFragBinding.electionSetupName;

        //Add text watchers on the fields that need to be filled
        electionNameText.addTextChangedListener(submitTextWatcher);

        // Set up the basic fields for ballot options, with at least two options

        // Set the text widget in layout to current LAO name
        TextView laoNameTextView = mSetupElectionFragBinding.electionSetupLaoName;
        laoNameTextView.setText(mLaoDetailViewModel.getCurrentLaoName().getValue());

        //Set viewPager adapter
        viewPagerAdapter = new ElectionSetupViewPagerAdapter(mLaoDetailViewModel);

        ViewPager2 viewPager2 = mSetupElectionFragBinding.electionSetupViewPager2;

        viewPager2.setAdapter(viewPagerAdapter);

        FloatingActionButton addQuestion = mSetupElectionFragBinding.addQuestion;
        addQuestion.setOnClickListener(v -> viewPagerAdapter.addQuestion());

        mSetupElectionFragBinding.setLifecycleOwner(getActivity());

        return mSetupElectionFragBinding.getRoot();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupElectionCancelButton();
        setupElectionSubmitButton();

        // subscribe to the election create event
        mLaoDetailViewModel
                .getElectionCreated()
                .observe(
                        this,
                        booleanEvent -> {
                            Boolean action = booleanEvent.getContentIfNotHandled();
                            if (action != null) {
                                mLaoDetailViewModel.openLaoDetail();
                            }
                        });
    }


    /**
     * Setups the submit button that creates the new election
     */
    private void setupElectionSubmitButton() {
        submitButton.setOnClickListener(
                v -> {
//                    //We "deactivate" the button on click, to prevent the user from creating multiple elections at once
//                    submitButton.setEnabled(false);
//                    //When submitting, we compute the timestamps for the selected start and end time
//                    computeTimesInSeconds();
//                    //Filter the list of ballot options to keep only non-empty fields
//                    List<String> filteredBallotOptions = new ArrayList<>();
//                    for (String ballotOption: ballotOptions) {
//                        if (!ballotOption.equals("")) filteredBallotOptions.add(ballotOption);
//                    }
//                    mLaoDetailViewModel.createNewElection(electionNameText.getText().toString(), startTimeInSeconds, endTimeInSeconds, votingMethod, mSetupElectionFragBinding.writeIn.isChecked(),
//                            filteredBallotOptions, electionQuestionText.getText().toString());
                });
    }

    /**
     * Setups the cancel button, that brings back to LAO detail page
     */
    private void setupElectionCancelButton() {
        Button cancelButton = mSetupElectionFragBinding.electionCancelButton;
        cancelButton.setOnClickListener(v -> mLaoDetailViewModel.openLaoDetail());
    }
}