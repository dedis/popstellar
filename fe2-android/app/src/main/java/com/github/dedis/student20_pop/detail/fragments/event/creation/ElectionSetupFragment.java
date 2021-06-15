package com.github.dedis.student20_pop.detail.fragments.event.creation;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.student20_pop.databinding.FragmentSetupElectionEventBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.detail.adapters.ElectionSetupViewPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import me.relex.circleindicator.CircleIndicator3;

public class ElectionSetupFragment extends AbstractEventCreationFragment{

    public static final String TAG = ElectionSetupFragment.class.getSimpleName();

    private FragmentSetupElectionEventBinding mSetupElectionFragBinding;

    //mandatory fields for submitting
    private CircleIndicator3 circleIndicator;
    private EditText electionNameText;
    private Button cancelButton;
    private Button submitButton;
    private ElectionSetupViewPagerAdapter viewPagerAdapter;
    private LaoDetailViewModel mLaoDetailViewModel;
    private ViewPager2 viewPager2;
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
                    submitButton.setEnabled(isElectionLevelInputValid() && viewPagerAdapter.isAnInputValid().getValue());}
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

        cancelButton = mSetupElectionFragBinding.electionCancelButton;
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

        viewPager2 = mSetupElectionFragBinding.electionSetupViewPager2;
        viewPager2.setAdapter(viewPagerAdapter);

        circleIndicator = mSetupElectionFragBinding.electionSetupSwipeIndicator;
        circleIndicator.setViewPager(viewPager2);

        viewPagerAdapter.isAnInputValid().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                submitButton.setEnabled(aBoolean && isElectionLevelInputValid());
            }
        });

        Button addQuestion = mSetupElectionFragBinding.addQuestion;
        addQuestion.setOnClickListener(v -> {
            addQuestion.setEnabled(false);
            viewPagerAdapter.addQuestion();
            circleIndicator.setViewPager(viewPager2);
            addQuestion.setEnabled(true);
        });

        mSetupElectionFragBinding.setLifecycleOwner(getActivity());

        hideButtonsOnKeyboardOpen();
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
                    //We "deactivate" the button on click, to prevent the user from creating multiple elections at once
                    submitButton.setEnabled(false);
                    //When submitting, we compute the timestamps for the selected start and end time
                    computeTimesInSeconds();
                    //Filter the list of ballot options to keep only non-empty fields
                    final List<Integer> validPositions = viewPagerAdapter.getValidInputs();

                    List<String> votingMethod = viewPagerAdapter.getVotingMethod();
                    List<String> questions = viewPagerAdapter.getQuestions();
                    List<List<String>> ballotsOptions = viewPagerAdapter.getBallotOptions();
                    List<String> votingMethodFiltered = new ArrayList<>();
                    List<String> questionsFiltered = new ArrayList<>();
                    List<List<String>> ballotsOptionsFiltered = new ArrayList<>();

                    //////////////////////////While write in not implemented///////////////////////////////////////////////
                    List<Boolean> writeIns = new ArrayList<>();
                    //////////////////////////////////////////////////////////////////////////////////////////////////////
                    for(Integer i : validPositions) {
                            writeIns.add(false); //While write in is not implemented
                            questionsFiltered.add(questions.get(i));
                            votingMethodFiltered.add(votingMethod.get(i));
                            List<String> questionBallotOptions = ballotsOptions.get(i);
                            List<String> filteredQuestionBallotOptions = new ArrayList<>();
                            for (String ballotOption : questionBallotOptions) {
                                if (!ballotOption.equals(""))
                                    filteredQuestionBallotOptions.add(ballotOption);
                            }
                            ballotsOptionsFiltered.add(filteredQuestionBallotOptions);
                        }
                    String electionName = electionNameText.getText().toString();
                    Log.d(TAG, "Creating election with name " + electionName + ", start time " + startTimeInSeconds + ", end time " + endTimeInSeconds + ", voting methods "
                    + votingMethodFiltered + ", writesIn " + writeIns + ", questions " + questionsFiltered + ", ballotsOptions " +ballotsOptionsFiltered);
                    mLaoDetailViewModel.createNewElection(electionName, startTimeInSeconds, endTimeInSeconds, votingMethodFiltered, writeIns,
                       ballotsOptionsFiltered, questionsFiltered);
                });
    }


    /**
     * Setups the cancel button, that brings back to LAO detail page
     */
    private void setupElectionCancelButton() {
        Button cancelButton = mSetupElectionFragBinding.electionCancelButton;
        cancelButton.setOnClickListener(v -> mLaoDetailViewModel.openLaoDetail());
    }

    /**
     * Adapted from https://blog.mindorks.com/how-to-check-the-visibility-of-software-keyboard-in-android
     */
    private void hideButtonsOnKeyboardOpen() {
        ConstraintLayout constraintLayout = mSetupElectionFragBinding.fragmentSetupElectionEvent;
        constraintLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                constraintLayout.getWindowVisibleDisplayFrame(rect);
                int screenHeight = constraintLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - rect.bottom;
                if (keypadHeight > screenHeight * 0.15) {
                    cancelButton.setVisibility(View.INVISIBLE);
                    submitButton.setVisibility(View.INVISIBLE);
                    circleIndicator.setVisibility(View.INVISIBLE);

                } else {
                    cancelButton.setVisibility(View.VISIBLE);
                    submitButton.setVisibility(View.VISIBLE);
                    circleIndicator.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean isElectionLevelInputValid(){
        return !electionNameText.getText().toString().trim().isEmpty() && !getStartDate().isEmpty()
                && !getStartTime().isEmpty() && !getEndDate().isEmpty() && !getEndTime().isEmpty();
    }
}