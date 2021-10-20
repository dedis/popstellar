package com.github.dedis.popstellar.ui.detail.event.election.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.CastVoteBallotOptionLayoutBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** This is where whe define behaviour of the ViewPager of election setup */
public class ElectionSetupViewPagerAdapter
    extends RecyclerView.Adapter<ElectionSetupViewPagerAdapter.ViewHolder> {

  public static final String TAG = ElectionSetupViewPagerAdapter.class.getSimpleName();
  LaoDetailViewModel mLaoDetailViewModel;
  private final List<String> votingMethod;
  private final List<List<String>> ballotOptions;
  private final List<Integer> numberBallotOptions;
  private final List<String> questions;
  private int numberOfQuestions;
  private Context context;
  private final Set<Integer> listOfValidQuestions;
  private final Set<Integer> listOfValidBallots;
  private final MutableLiveData<Boolean> isAnInputValid;

  public ElectionSetupViewPagerAdapter(LaoDetailViewModel mLaoDetailViewModel) {
    super();
    this.mLaoDetailViewModel = mLaoDetailViewModel;
    votingMethod = new ArrayList<>();
    ballotOptions = new ArrayList<>();
    numberBallotOptions = new ArrayList<>();
    questions = new ArrayList<>();
    listOfValidBallots = new HashSet<>();
    listOfValidQuestions = new HashSet<>();
    isAnInputValid = new MutableLiveData<>(Boolean.valueOf(false));
    addQuestion();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    context = parent.getContext();
    return new ViewHolder(
        LayoutInflater.from(context)
            .inflate(R.layout.election_setup_question_layout, parent, false));
  }

  @Override
  public int getItemViewType(int position) {
    return position;
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    // The holder represents the view of one page of the pager

    // This is bad practice and should be removed in the future
    // The problem for now is that reused view messes up the data intake
    holder.setIsRecyclable(false);

    EditText electionQuestionText = holder.electionQuestionText;

    ballotOptions.set(position, new ArrayList<>());
    electionQuestionText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            /* no check to make before text is changed */
          }

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            /* no check to make before text is changed */
          }

          @Override
          public void afterTextChanged(Editable s) {
            // On each text change we edit the list of questions
            // and we add or remove the question from the list of filled question
            String questionText = s.toString();
            if (!electionQuestionText.getText().toString().trim().isEmpty()) {
              questions.set(position, questionText);
              listOfValidQuestions.add(position);
            } else {
              listOfValidQuestions.remove(position);
            }
            checkIfAnInputIsValid();
          }
        });

    // Setting the spinner and its listener
    Spinner spinner = holder.spinner;
    AdapterView.OnItemSelectedListener spinnerListener =
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
            String elementToAdd = parent.getItemAtPosition(i).toString();
            if (votingMethod.size() <= position) {
              votingMethod.add(elementToAdd);
            }
            votingMethod.set(position, elementToAdd);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {
            votingMethod.set(position, "Plurality");
          }
        };
    setupElectionSpinner(spinner, spinnerListener);

    Button addBallotOptionButton = holder.addOptionButton;

    LinearLayout linearLayout = holder.linearLayout;
    addBallotOptionButton.setOnClickListener(v -> addBallotOption(linearLayout, position));

    // Minimum for each question is two ballots
    addBallotOption(linearLayout, position);
    addBallotOption(linearLayout, position);
  }

  /** On each question we add, we expand the lists appropriately */
  public void addQuestion() {
    numberOfQuestions++;
    votingMethod.add("");
    questions.add("");
    ballotOptions.add(new ArrayList<>());
    numberBallotOptions.add(0);
    notifyItemInserted(numberOfQuestions - 1);
  }

  @Override
  public int getItemCount() {
    // This defines the number of page we will have
    return numberOfQuestions;
  }

  public MutableLiveData<Boolean> isAnInputValid() {
    return isAnInputValid;
  }

  /**
   * Checks if an input is valid by crosschecking the list of election questions where the question
   * and the ballots are filled
   */
  public void checkIfAnInputIsValid() {
    for (Integer i : listOfValidQuestions) {
      if (listOfValidBallots.contains(i)) {
        isAnInputValid.setValue(true);
        return;
      }
    }
    isAnInputValid.setValue(false);
  }

  /** @return the indexes of election question where input is valid */
  public List<Integer> getValidInputs() {
    List<Integer> listOfValidInputs = new ArrayList<>();
    for (Integer i : listOfValidQuestions) {
      if (listOfValidBallots.contains(i)) {
        listOfValidInputs.add(i);
      }
    }
    return listOfValidInputs;
  }

  /** Adds a view of ballot option to the layout when user clicks the button */
  private void addBallotOption(LinearLayout linearLayout, int position) {
    // Adds the view for a new ballot option, from the corresponding layout
    View ballotOptionView =
        CastVoteBallotOptionLayoutBinding.inflate(LayoutInflater.from(context)).newBallotOptionLl;
    EditText ballotOptionText = ballotOptionView.findViewById(R.id.new_ballot_option_text);
    linearLayout.addView(ballotOptionView);

    // Gets the index associated to the ballot option's view
    int ballotIndex = linearLayout.indexOfChild(ballotOptionView);
    List<String> questionBallotOption = ballotOptions.get(position);
    ballotOptions.get(position).add("");
    ballotOptionText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            /* no check to make before text is changed */
          }

          @Override
          public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            /*no check to make during the text is being changed */
          }

          @Override
          public void afterTextChanged(Editable editable) {
            String text = editable.toString();
            // Prevents the user from creating two different ballot options with the same name
            if (!text.isEmpty() && questionBallotOption.contains(text)) {
              ballotOptionText.setError("Two different ballot options can't have the same name");
            }
            // Counts the number of non-empty ballot options, to know when the user can create the
            // election (at least 2 non-empty)
            if (questionBallotOption.isEmpty()
                || (questionBallotOption.get(ballotIndex).isEmpty() && !text.isEmpty())) {
              numberBallotOptions.set(position, numberBallotOptions.get(position) + 1);
            } else if (!questionBallotOption.get(ballotIndex).isEmpty() && text.isEmpty()) {
              numberBallotOptions.set(position, numberBallotOptions.get(position) - 1);
            }
            // Keeps the list of string updated when the user changes the text
            ballotOptions.get(position).set(ballotIndex, editable.toString());
            Log.d(TAG, "Postion is " + position + " ballot options are" + ballotOptions.toString());
            boolean areFieldsFilled = numberBallotOptions.get(position) >= 2;
            if (areFieldsFilled) {
              listOfValidBallots.add(position);
            } else {
              listOfValidBallots.remove(position);
            }
            checkIfAnInputIsValid();
          }
        });
  }

  private void setupElectionSpinner(Spinner spinner, AdapterView.OnItemSelectedListener listener) {

    String[] items =
        Arrays.stream(ElectionSetupFragment.VotingMethods.values())
            .map(ElectionSetupFragment.VotingMethods::getDesc)
            .toArray(String[]::new);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(listener);
  }

  public List<String> getVotingMethod() {
    return votingMethod;
  }

  public List<List<String>> getBallotOptions() {
    return ballotOptions;
  }

  public List<String> getQuestions() {
    return questions;
  }

  public int getNumberOfQuestions() {
    return numberOfQuestions;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private EditText electionQuestionText;
    private Spinner spinner;
    private Button addOptionButton;
    private LinearLayout linearLayout;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      electionQuestionText = itemView.findViewById(R.id.election_question);
      spinner = itemView.findViewById(R.id.election_setup_spinner);
      addOptionButton = itemView.findViewById(R.id.add_ballot_option);
      linearLayout = itemView.findViewById(R.id.election_setup_ballot_options_ll);
    }
  }
}
