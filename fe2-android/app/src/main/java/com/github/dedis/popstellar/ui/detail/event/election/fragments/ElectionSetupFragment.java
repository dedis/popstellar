package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ElectionSetupFragmentBinding;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.ui.detail.*;
import com.github.dedis.popstellar.ui.detail.event.AbstractEventCreationFragment;
import com.github.dedis.popstellar.ui.detail.event.election.ZoomOutTransformer;
import com.github.dedis.popstellar.ui.detail.event.election.adapters.ElectionSetupViewPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import me.relex.circleindicator.CircleIndicator3;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

@AndroidEntryPoint
public class ElectionSetupFragment extends AbstractEventCreationFragment {

  public static final String TAG = ElectionSetupFragment.class.getSimpleName();

  private ElectionSetupFragmentBinding mSetupElectionFragBinding;

  // mandatory fields for submitting
  private EditText electionNameText;
  private Button cancelButton;
  private Button submitButton;
  private ElectionSetupViewPagerAdapter viewPagerAdapter;
  private LaoDetailViewModel mLaoDetailViewModel;

  // For election version choice
  private ElectionVersion electionVersion;

  // Enum of all voting methods, associated to a string desc for protocol and spinner display
  public enum VotingMethods {
    PLURALITY("Plurality");
    private final String desc;

    VotingMethods(String desc) {
      this.desc = desc;
    }

    public String getDesc() {
      return desc;
    }
  }

  // Text watcher that checks if mandatory fields are filled for submitting each time the user
  // changes a field (with at least two valid ballot options)
  private final TextWatcher submitTextWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          /* no check to make before text is changed */
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          /* no check to make during the text is being changes */
        }

        @Override
        public void afterTextChanged(Editable s) {
          // On each change of election level information, we check that at least one question is
          // complete to know if submit is allowed
          submitButton.setEnabled(
              isElectionLevelInputValid() && viewPagerAdapter.isAnInputValid().getValue());
        }
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

    mSetupElectionFragBinding = ElectionSetupFragmentBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    // Set the view for the date and time
    setDateAndTimeView(mSetupElectionFragBinding.getRoot());
    // Make the textWatcher listen to changes in the start and end date/time
    addEndDateAndTimeListener(submitTextWatcher);
    addStartDateAndTimeListener(submitTextWatcher);

    cancelButton = mSetupElectionFragBinding.electionCancelButton;
    submitButton = mSetupElectionFragBinding.electionSubmitButton;
    electionNameText = mSetupElectionFragBinding.electionSetupName;

    // Add text watchers on the fields that need to be filled
    electionNameText.addTextChangedListener(submitTextWatcher);

    // Set the text widget in layout to current LAO name
    TextView laoNameTextView = mSetupElectionFragBinding.electionSetupLaoName;
    laoNameTextView.setText(mLaoDetailViewModel.getCurrentLaoName().getValue());

    // Set viewPager adapter
    viewPagerAdapter = new ElectionSetupViewPagerAdapter(mLaoDetailViewModel);

    // Set ViewPager
    ViewPager2 viewPager2 = mSetupElectionFragBinding.electionSetupViewPager2;
    viewPager2.setAdapter(viewPagerAdapter);

    // Sets animation on swipe
    viewPager2.setPageTransformer(new ZoomOutTransformer());

    // This sets the indicator of which page we are on
    CircleIndicator3 circleIndicator = mSetupElectionFragBinding.electionSetupSwipeIndicator;
    circleIndicator.setViewPager(viewPager2);

    // This observes if at least one of the question has the minimal information
    viewPagerAdapter
        .isAnInputValid()
        .observe(
            getViewLifecycleOwner(),
            aBoolean -> submitButton.setEnabled(aBoolean && isElectionLevelInputValid()));

    Button addQuestion = mSetupElectionFragBinding.addQuestion;
    addQuestion.setOnClickListener(
        v -> {
          addQuestion.setEnabled(false);
          viewPagerAdapter.addQuestion();

          // This scales for a few dozens of questions but this is dangerous and  greedy in
          // resources
          // TODO delete this and find a way to keep data on left swipe
          viewPager2.setOffscreenPageLimit(viewPagerAdapter.getNumberOfQuestions());

          // This swipes automatically to new question
          viewPager2.setCurrentItem(viewPager2.getCurrentItem() + 1);

          // Updates the number of circles in the indicator
          circleIndicator.setViewPager(viewPager2);
          addQuestion.setEnabled(true);
        });

    // Create a listener that updates the user's choice for election (by default it's OPEN_BALLOT)
    // Then it set's up the spinner
    Spinner versionSpinner = mSetupElectionFragBinding.electionSetupModeSpinner;
    OnItemSelectedListener listener =
        new OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position == 0) {
              electionVersion = ElectionVersion.OPEN_BALLOT;
            } else if (position == 1) {
              electionVersion = ElectionVersion.SECRET_BALLOT;
            }
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            electionVersion = ElectionVersion.OPEN_BALLOT;
          }
        };
    setUpElectionVersionSpinner(versionSpinner, listener);

    mSetupElectionFragBinding.setLifecycleOwner(getActivity());

    return mSetupElectionFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupElectionCancelButton();
    setupElectionSubmitButton();
  }

  /** Setups the submit button that creates the new election */
  private void setupElectionSubmitButton() {
    submitButton.setOnClickListener(
        v -> {
          // We "deactivate" the button on click, to prevent the user from creating multiple
          // elections at once
          submitButton.setEnabled(false);

          // When submitting, we compute the timestamps for the selected start and end time
          if (!computeTimesInSeconds()) {
            return;
          }

          final List<Integer> validPositions = viewPagerAdapter.getValidInputs();

          List<String> votingMethod = viewPagerAdapter.getVotingMethod();
          List<String> questions = viewPagerAdapter.getQuestions();
          List<List<String>> ballotsOptions = viewPagerAdapter.getBallotOptions();
          List<String> votingMethodFiltered = new ArrayList<>();
          List<String> questionsFiltered = new ArrayList<>();
          List<List<String>> ballotsOptionsFiltered = new ArrayList<>();

          ////////////////////////// While write in not
          // implemented///////////////////////////////////////////////
          List<Boolean> writeIns = new ArrayList<>();
          //////////////////////////////////////////////////////////////////////////////////////////////////////
          for (Integer i : validPositions) {
            // We filter to only take the questions for which all data is filled

            writeIns.add(false); // While write in is not implemented

            questionsFiltered.add(questions.get(i));
            votingMethodFiltered.add(votingMethod.get(i));
            List<String> questionBallotOptions = ballotsOptions.get(i);
            List<String> filteredQuestionBallotOptions = new ArrayList<>();
            for (String ballotOption : questionBallotOptions) {
              // Filter the list of ballot options to keep only non-empty fields
              if (!ballotOption.equals("")) {
                filteredQuestionBallotOptions.add(ballotOption);
              }
            }
            ballotsOptionsFiltered.add(filteredQuestionBallotOptions);
          }

          String electionName = electionNameText.getText().toString();

          Log.d(
              TAG,
              "Creating election with version "
                  + electionVersion
                  + ", name "
                  + electionName
                  + ", creation time "
                  + creationTimeInSeconds
                  + ", start time "
                  + startTimeInSeconds
                  + ", end time "
                  + endTimeInSeconds
                  + ", voting methods "
                  + votingMethodFiltered
                  + ", writesIn "
                  + writeIns
                  + ", questions "
                  + questionsFiltered
                  + ", ballotsOptions "
                  + ballotsOptionsFiltered);
          mLaoDetailViewModel.createNewElection(
              getParentFragmentManager(),
              electionVersion,
              electionName,
              creationTimeInSeconds,
              startTimeInSeconds,
              endTimeInSeconds,
              votingMethodFiltered,
              writeIns,
              ballotsOptionsFiltered,
              questionsFiltered);
        });
  }

  /** Setups the cancel button, that brings back to LAO detail page */
  private void setupElectionCancelButton() {
    cancelButton = mSetupElectionFragBinding.electionCancelButton;
    cancelButton.setOnClickListener(
        v ->
            setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_lao_detail,
                LaoDetailFragment::newInstance));
  }

  /**
   * @return true if the election name text, dates and times inputs are valid
   */
  private boolean isElectionLevelInputValid() {
    return !electionNameText.getText().toString().trim().isEmpty()
        && !getStartDate().isEmpty()
        && !getStartTime().isEmpty()
        && !getEndDate().isEmpty()
        && !getEndTime().isEmpty();
  }

  /**
   * Sets up the dropdown menu for election versions: open-ballot and secret-ballot
   *
   * @param spinner the spinner to modify
   * @param listener listener to spinner event
   */
  private void setUpElectionVersionSpinner(
      Spinner spinner, AdapterView.OnItemSelectedListener listener) {

    List<ElectionVersion> versionsList = ElectionVersion.getAllElectionVersion();
    List<String> items = new ArrayList<>();

    // Add items to version list
    for (ElectionVersion v : versionsList) {
      items.add(v.getStringBallotVersion());
    }

    // Set up the spinner with voting versions
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(listener);
  }
}
